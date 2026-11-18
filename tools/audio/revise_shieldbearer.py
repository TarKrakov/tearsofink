#!/usr/bin/env python3
"""D-070: shieldbearer voice, roar and physical impacts. No external samples.
Run at repo root with numpy, scipy, soundfile. Original OGG sizes/rates retained
except charge-start, which is explicitly extended for the one-shot roar."""
from pathlib import Path
import hashlib
import json
import tempfile
import numpy as np
from scipy import signal, ndimage
import soundfile as sf

ROOT=Path("src/main/resources/assets/tears-of-ink/sounds")
NAMES={
    "ambient_01","ambient_02","hurt_01","death_01",
    "charge_prepare_01","charge_prepare_02","charge_start_01",
    "charge_hit_01","charge_break_soft_01","charge_wall_01",
    "bash_hit_01","bash_hit_02","stun_01",
}
FILES={"pale_shield_"+n+".ogg" for n in NAMES}

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def rms(x): return float(np.sqrt(np.mean(x*x)))
def unit(x): return x/max(np.max(np.abs(x)),1e-10)
def band(x,sr,lo,hi):
    return signal.sosfilt(signal.butter(2,[lo,min(hi,sr*.45)],btype="bandpass",fs=sr,output="sos"),x)
def lp(x,sr,hi): return signal.sosfilt(signal.butter(2,hi,fs=sr,output="sos"),x)
def taper(u): return np.sin(np.pi*np.clip(u,0,1))**2
def put(dst,src,at,sr,gain=1.):
    i=max(0,round(at*sr)); n=min(len(dst)-i,len(src))
    if n>0: dst[i:i+n]+=src[:n]*gain

def voice(n,sr,rng,style):
    # Source/filter: irregular vocal-fold closure impulses, mixed breath,
    # resonant throat/vowel cavities. Not an oscillator note + noise wash.
    t=np.arange(n)/sr; D=n/sr; u=t/D
    patterns={
        "idle1": ([65,72,62,55],[0,.65,.82,0],[(310,760,1420),(420,920,1600)]),
        "idle2": ([59,76,67,49],[0,.85,.50,0],[(390,870,1500),(290,690,1330)]),
        "hurt": ([81,97,69,53],[0,1,.5,0],[(520,1080,1760),(350,800,1460)]),
        "death": ([76,67,55,39],[0,.95,.65,0],[(440,890,1540),(270,670,1200)]),
        "brace1": ([53,65,73,60],[0,.42,.9,0],[(310,710,1350),(420,870,1540)]),
        "brace2": ([58,71,64,68],[0,.50,.95,0],[(360,800,1510),(440,930,1620)]),
        "roar": ([69,96,88,56],[0,1,.95,0],[(630,1080,1790),(395,820,1470)]),
        "stun": ([71,58,64,43],[0,.8,.38,0],[(480,970,1620),(300,710,1280)]),
    }
    pitches,amps,formants=patterns[style]
    f0=np.interp(u,[0,.24,.59,1],pitches)
    f0*=1+.06*np.sin(2*np.pi*(8.1*t+.21))+ .035*np.sin(2*np.pi*13.3*t)
    glottis=np.zeros(n); start=int(.015*sr); j=0
    while start<n:
        period=sr/f0[start]*rng.uniform(.84,1.16)
        width=max(4,int(period*rng.uniform(.29,.55)))
        a=np.arange(width)/width
        # Glottal flow derivative, an asymmetric open/close cycle.
        pulse=np.gradient(np.sin(np.pi*a)**1.65)
        pulse=unit(pulse)*rng.uniform(.72,1.22)*(1 if j%2 else .65)
        put(glottis,pulse,start/sr,sr)
        start+=max(1,int(period)); j+=1
    glottis=lp(glottis,sr,3700)
    raw=unit(glottis)
    # Irregular voicing gives throat texture, not a continuous rustling layer.
    raw+=.07*unit(band(rng.standard_normal(n),sr,200,2200))*np.minimum(1,2*np.abs(raw))
    def form(f):
        out=np.zeros(n)
        for freq,bw,g in zip(f,[190,280,425],[1.,.55,.22]):
            out+=g*unit(band(raw,sr,max(70,freq-bw/2),freq+bw/2))
        return out
    mix=np.clip((u-.16)/.67,0,1)
    throat=(1-mix)*form(formants[0])+mix*form(formants[1])
    chest=unit(band(raw,sr,50,360))
    y=np.tanh(1.7*(.75*throat+.52*chest))
    env=np.interp(u,[0,.12,.48,1],amps)
    env=ndimage.gaussian_filter1d(env,max(1,sr*.008))
    if style=="roar":
        env=np.interp(u,[0,.045,.18,.48,.73,1],[0,.25,1,.95,.65,0])
        env*=.9+.1*np.sin(2*np.pi*10.2*t)
    elif style=="death":
        env*=.8+.2*np.sin(2*np.pi*5.4*t)
    elif style.startswith("idle"):
        env*=.78+.22*np.sin(2*np.pi*3.7*t)
    return y*env

def burst(sr,rng,length,lo,hi,decay,attack=.0015):
    n=max(8,round(sr*length)); t=np.arange(n)/sr
    # Contact energy is turbulent and aperiodic, confined to the impact.
    z=unit(band(rng.standard_normal(n),sr,lo,hi))
    e=(1-np.exp(-t/attack))*np.exp(-t/decay)
    e*=np.clip((length-t)/.009,0,1)
    return z*e

def flex(sr,rng,length,base,decay):
    n=max(8,round(sr*length)); t=np.arange(n)/sr
    # Damped flex of a heavy body; rapid frequency descent with no sustained pitch.
    y=np.zeros(n)
    for ratio,g in [(1,1),(1.81,.30),(2.71,.14)]:
        f=base*ratio*rng.uniform(.90,1.10)
        phi=2*np.pi*(f*t+f*.62*.018*(1-np.exp(-t/.018)))
        y+=g*np.sin(phi+rng.uniform(-.3,.3))*np.exp(-t/decay)
    return unit(y)*(1-np.exp(-t/.0015))*np.clip((length-t)/.012,0,1)

def impact(n,sr,rng,style):
    y=np.zeros(n); D=n/sr
    def material(at,length,lo,hi,decay,gain):
        put(y,burst(sr,rng,length,lo,hi,decay),at,sr,gain)
    def body(at,f,decay,gain):
        put(y,flex(sr,rng,min(.22,D),f,decay),at,sr,gain)
    if style in ("target","bash1","bash2"):
        # Shield against flesh/armor: one broad contact and displaced mass.
        heavy=style=="target"
        body(.003,46 if heavy else 65,.058 if heavy else .036,1.)
        material(.001,.10,78,510,.026,.93)
        material(.003,.045,480,2600,.008,.85)
        material(.023,.065,240,1100,.018,.42)
        if style=="bash2": material(.04,.035,900,4500,.009,.40)
        else: material(.012,.04,1200,3700,.009,.30)
        if heavy:
            body(.046,79,.034,.45)
            material(.055,.060,180,780,.016,.43)
    elif style=="wall":
        # Stone and whole shield vibrate, then irregular fragments/clattering.
        body(.002,41,.081,1.0)
        material(.001,.14,90,790,.036,1.2)
        material(.004,.07,650,5400,.017,.92)
        for i in range(9):
            at=.028+i*.015+rng.uniform(0,.017)
            material(at,.042,460,5000,.008,.48*np.exp(-i/4))
        body(.063,92,.037,.32)
    elif style=="break":
        # Splintering obstacle: discontinuous cracks in several directions.
        body(.003,67,.044,.80)
        material(.002,.080,110,1150,.024,.80)
        for i in range(12):
            at=.007+i*.012+rng.uniform(0,.019)
            material(at,.036,950,6800,.0038,.80*np.exp(-i/7))
            if i%3==0: material(at+.009,.056,220,1500,.012,.35)
    elif style=="brace":
        body(.005,78,.035,.50)
        material(.007,.065,190,1700,.018,.56)
        material(.035,.06,600,4200,.009,.4)
    elif style=="launch":
        body(.001,56,.055,.78)
        material(.006,.087,110,900,.024,.72)
        material(.028,.044,480,3400,.011,.35)
    elif style=="stun":
        body(.002,75,.041,.62)
        material(.003,.07,340,2100,.018,.5)
        material(.047,.06,620,3400,.017,.38)
    else: raise ValueError(style)
    return y

def render(name,n,sr,rng):
    D=n/sr
    if name.startswith("ambient"):
        return voice(n,sr,rng,"idle1" if name.endswith("01") else "idle2")
    if name.startswith("hurt"): return voice(n,sr,rng,"hurt")
    if name.startswith("death"): return voice(n,sr,rng,"death")
    if name.startswith("charge_prepare"):
        y=voice(n,sr,rng,"brace1" if name.endswith("01") else "brace2")*.72
        z=impact(round(min(.2,D)*sr),sr,rng,"brace")
        put(y,z,.005,sr,.42)
        return y
    if name.startswith("charge_start"):
        # Start is already a one-shot at CHARGE_WINDUP, no roar loop/spam.
        y=voice(n,sr,rng,"roar")
        z=impact(round(min(.24,D)*sr),sr,rng,"launch")
        put(y,z,.006,sr,.38)
        return y
    if name.startswith("stun"):
        y=impact(n,sr,rng,"stun")*.72
        put(y,voice(round(n*.83),sr,rng,"stun"),D*.12,sr,.45)
        return y
    return impact(n,sr,rng,{"charge_hit_01":"target","charge_wall_01":"wall",
        "charge_break_soft_01":"break","bash_hit_01":"bash1","bash_hit_02":"bash2"}[name])

def match(y,x,sr,name):
    # Match approximate RMS, but use a conservative peak ceiling. With a
    # longer roar, match level rather than the old short impact's duration.
    y=y-np.mean(y)
    n=min(len(y)//6,round(sr*.008))
    y[:n]*=np.linspace(0,1,n); y[-n:]*=np.linspace(1,0,n)
    target=max(rms(x),.012)
    if name.startswith("charge_start"): target=max(target,.040)
    y*=min(target/max(rms(y),1e-10),.72/max(np.max(np.abs(y)),1e-10))
    return y

def main():
    all_files={p.name:p for p in ROOT.glob("*.ogg")}
    assert FILES<=set(all_files)
    frozen={n:sha(p) for n,p in all_files.items() if n not in FILES}
    report=[]
    with tempfile.TemporaryDirectory() as tmp:
        tmp=Path(tmp)
        for FN in sorted(FILES):
            p=all_files[FN]; name=p.stem.removeprefix("pale_shield_")
            x,sr=sf.read(p,dtype="float64"); assert x.ndim==1
            n=max(len(x),round(sr*.72)) if name=="charge_start_01" else len(x)
            rng=np.random.default_rng(int.from_bytes(hashlib.sha256(("D070:"+FN).encode()).digest()[:8],"big"))
            y=match(render(name,n,sr,rng),x,sr,name)
            assert np.isfinite(y).all() and rms(y)>.003
            out=tmp/FN; sf.write(out,y,sr,format="OGG",subtype="VORBIS")
            z,zsr=sf.read(out,dtype="float64")
            assert z.ndim==1 and len(z)==n and zsr==sr and np.isfinite(z).all()
            assert np.max(np.abs(z))<.98 and rms(z)>max(.003,rms(x)*.20)
            report.append({"file":FN,"rate":sr,"duration_before":len(x)/sr,"duration_after":n/sr,
                "rms_before":20*np.log10(rms(x)),"rms_after":20*np.log10(rms(z)),
                "peak_after":20*np.log10(np.max(np.abs(z))),"before":sha(p),"after":sha(out)})
        for FN in FILES: all_files[FN].write_bytes((tmp/FN).read_bytes())
    assert all(sha(all_files[n])==v for n,v in frozen.items())
    p=Path("out/audio-d070.json"); p.parent.mkdir(exist_ok=True,parents=True)
    p.write_text(json.dumps({"changed":report,"untouched":frozen},indent=2)+"\n")
    print("Verified",len(report),"shieldbearer samples and",len(frozen),"unchanged audio files.")
    for r in report: print(r["file"],round(r["duration_after"],3),round(r["rms_after"],1),round(r["peak_after"],1))

if __name__=="__main__": main()
