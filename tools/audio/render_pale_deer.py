#!/usr/bin/env python3
"""D-076: original procedural cervid audio, no borrowed samples.
Run once from the repository root; never overwrite authored audio.
"""
import hashlib
import json
from pathlib import Path
import numpy as np
from scipy import signal
import soundfile as sf

SR = 44100
ROOT = Path("src/main/resources/assets/tears-of-ink/sounds")
# Durations are below the authored one-shot clips, including pitch variation.
DURATIONS = {
    "voice": (.84, 1.04, 1.20),
    "snort": (.22, .30, .38),
    "step": (.17, .20, .19),
    "eat": (.32, .38, .29),
    "alert_snort": (.34, .39, .46),
    "watch_roar": (1.48, 1.53, 1.49),
    "roar": (1.16, 1.24, 1.20),
    "run_step": (.23, .24, .21),
}
def seed(name):
    return int.from_bytes(hashlib.sha256(("TOI:D076:"+name).encode()).digest()[:8], "big")
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
def rms(x):
    return float(np.sqrt(np.mean(x*x)))
def norm(x):
    return x / max(float(np.max(np.abs(x))), 1e-9)
def bp(x, lo, hi):
    return signal.sosfilt(signal.butter(2, [lo, hi], fs=SR, btype="bandpass", output="sos"), x)
def smooth_curve(t, xs, ys):
    from scipy.interpolate import PchipInterpolator
    return PchipInterpolator(xs, ys)(t)
def add(dest, src, seconds, gain=1):
    i = round(seconds * SR)
    n = min(len(src), len(dest)-i)
    if n > 0:
        dest[i:i+n] += gain*src[:n]
def time(d):
    return np.arange(round(d*SR))/SR

def bellow(d, rng, kind):
    # Cervid-like glottal source: asymmetrical, aperiodic vocal-fold pulses,
    # changing nasal/oral resonances. No musical chord, pitched synth hit or noise bed.
    t=time(d); u=t/d; n=len(t)
    contours = {
      "voice": ([105, 139, 115, 78], [0,.65,.80,.0], [400,910,1660,2600], [310,730,1390,2300]),
      "watch_roar": ([102, 158, 134, 69], [0,.95,1,0], [570,1150,2050,3050], [360,800,1550,2460]),
      "roar": ([115, 167, 123, 78], [0,.9,.85,0], [510,1040,1820,2820], [340,770,1460,2350]),
    }
    pitches,levels,fa,fb=contours[kind]
    f=smooth_curve(u,[0,.23,.61,1],pitches)*rng.uniform(.85,1.15)
    f*=1+.045*np.sin(2*np.pi*(6.5*t+rng.random()))+.025*np.sin(2*np.pi*17.3*t)
    # Pulse jitter and intermittent subharmonic weight provide a coarse animal throat.
    excitation=np.zeros(n)
    i=round(.009*SR); j=0
    while i<n:
        period=SR/f[i]*rng.uniform(.91,1.09)
        width=max(6,round(period*rng.uniform(.44,.70)))
        v=np.arange(width)/width
        pulse=np.gradient(np.sin(np.pi*v)**2.1)
        amplitude=rng.uniform(.75,1.22)*(1 if j%2==0 else rng.uniform(.45,.90))
        m=min(width,n-i)
        excitation[i:i+m]+=amplitude*norm(pulse)[:m]
        i+=max(1,round(period)); j+=1
    excitation=norm(excitation)
    def tract(formants):
        y=np.zeros(n)
        for hz,bw,g in zip(formants,[160,230,350,580],[1,.68,.32,.12]):
            hz*=rng.uniform(.96,1.04)
            y+=g*norm(bp(excitation,hz-bw/2,hz+bw/2))
        return y
    mix=smooth_curve(u,[0,.18,.67,1],[0,.15,.8,1])
    chest=norm(bp(excitation,75,360))
    nasal=norm(bp(excitation,760,1550))
    y=(1-mix)*tract(fa)+mix*tract(fb)+.47*chest+.13*nasal
    # Breath only follows active vocal folds; it is not a separate rustling layer.
    y+=.055*norm(bp(rng.standard_normal(n),350,3800))*np.minimum(1,2*np.abs(y))
    env=smooth_curve(u,[0,.10,.40,.73,1],[0,levels[1]*.48,levels[1],levels[2]*.62,0])
    env*=.84+.16*np.sin(2*np.pi*(5.7*t+rng.random()))
    return np.tanh(y*1.15)*env

def puff(d, rng, alert):
    # Brief explosive nostril exhalation, two unequal lobes, no continuous noise.
    t=time(d); y=np.zeros(len(t))
    times=[.008,d*.36] if alert else [.006,d*.40]
    for i,at in enumerate(times):
        z=t-at; positive=np.maximum(z,0)
        env=(1-np.exp(-positive/.007))*np.exp(-positive/(.038 if alert else .030))*(z>=0)
        breath=norm(bp(rng.standard_normal(len(t)),420 if alert else 240,2900 if alert else 1900))
        nasal=norm(bp(breath,680,1150))
        y+=(1 if i==0 else .48)*env*(.78*breath+.40*nasal)
    # Short flutter from nostril/tissue vibration, not pitched beep.
    y*=.75+.25*np.sin(2*np.pi*rng.uniform(25,38)*t)
    return y

def hoof(d,rng,heavy):
    # Bone/horn contact on a soft substrate: sharp dry clop followed by a low damped body.
    t=time(d); y=np.zeros(len(t))
    for a,g in [(0,1), (rng.uniform(.026,.053),rng.uniform(.38,.65))]:
        v=np.maximum(t-a,0); active=t>=a
        attack=(1-np.exp(-v/.0012))*active
        body=np.zeros(len(t))
        for f,w in [(82 if heavy else 115,1),(195,.50),(420,.19)]:
            f*=rng.uniform(.83,1.17)
            body+=w*np.sin(2*np.pi*(f*v+f*.18*.012*(1-np.exp(-v/.012))))*np.exp(-v/(.038 if heavy else .022))
        clack=norm(bp(rng.standard_normal(len(t)),1000,5100))*np.exp(-v/.0048)
        dirt=norm(bp(rng.standard_normal(len(t)),90,620))*np.exp(-v/.022)
        y+=g*attack*(.78*body+.58*clack+.42*dirt)
    return y

def chew(d,rng):
    # Tiny wet/wiry stem snaps and jaw pressure, not a foliage wash.
    t=time(d); y=np.zeros(len(t))
    for a in [0.018,.078,.19]:
        a*=rng.uniform(.80,1.15)
        v=np.maximum(t-a,0); env=(1-np.exp(-v/.002))*np.exp(-v/.012)*(t>=a)
        snap=norm(bp(rng.standard_normal(len(t)),950,4100))
        jaw=norm(bp(rng.standard_normal(len(t)),180,850))*np.exp(-v/.025)
        y+=env*(.65*snap+.45*jaw)*rng.uniform(.4,1)
    return y

def main():
    ROOT.mkdir(parents=True,exist_ok=True)
    before={p.name:digest(p) for p in ROOT.glob("*.ogg")}
    report=[]
    expected={f"pale_deer_{kind}_{i:02d}.ogg" for kind,ds in DURATIONS.items() for i in range(1,len(ds)+1)}
    assert not expected.intersection(before), "Refusing to replace existing deer audio"
    for kind,ds in DURATIONS.items():
        for i,d in enumerate(ds,1):
            name=f"pale_deer_{kind}_{i:02d}.ogg"
            rng=np.random.default_rng(seed(name))
            if kind in ("voice","watch_roar","roar"): y=bellow(d,rng,kind)
            elif kind in ("snort","alert_snort"): y=puff(d,rng,kind=="alert_snort")
            elif kind in ("step","run_step"): y=hoof(d,rng,kind=="run_step")
            else: y=chew(d,rng)
            y=y-np.mean(y)
            fade=min(round(.007*SR),len(y)//4)
            y[:fade]*=np.linspace(0,1,fade); y[-fade:]*=np.linspace(1,0,fade)
            # Conservative individual peak; collective level is also compensated at runtime.
            target=.11 if kind in ("watch_roar","roar") else .085 if kind=="voice" else .063
            y*=min(target/max(rms(y),1e-9),.55/max(np.max(np.abs(y)),1e-9))
            assert np.isfinite(y).all()
            path=ROOT/name
            sf.write(path,y,SR,format="OGG",subtype="VORBIS")
            z,rate=sf.read(path,dtype="float64"); info=sf.info(path)
            assert info.subtype=="VORBIS" and info.channels==1 and rate==SR and len(z)==len(y)
            assert np.isfinite(z).all() and rms(z)>.005 and np.max(np.abs(z))<.85
            report.append({"file":name,"seconds":len(z)/rate,"peak_dbfs":round(20*np.log10(np.max(np.abs(z))),2),
                           "rms_dbfs":round(20*np.log10(rms(z)),2),"sha256":digest(path)})
    assert all(digest(ROOT/n)==sha for n,sha in before.items())
    assert len({r["sha256"] for r in report})==len(expected)==24
    assert {p.name for p in ROOT.glob("*.ogg")}-set(before)==expected
    out=Path("out/audio-d076.json"); out.parent.mkdir(exist_ok=True)
    out.write_text(json.dumps({"samples":report,"unchanged_ogg":len(before)},indent=2)+"\n")
    print("Verified",len(report),"new mono Vorbis samples;",len(before),"existing OGG unchanged")

if __name__=="__main__":
    main()
