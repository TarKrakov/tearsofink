#!/usr/bin/env python3
"""D-069: source-specific audio cleanup. Offline, deterministic, no game-code changes.
Run from repository root: python tools/audio/revise_timbres.py
Requires numpy, scipy, soundfile (libsndfile with Vorbis).
"""
from pathlib import Path
import hashlib
import json
import re
import tempfile
import numpy as np
from scipy import signal, ndimage
import soundfile as sf

ROOT = Path("src/main/resources/assets/tears-of-ink/sounds")
SEED = "D-069-v1"
REPORT = Path("out/audio-d069.json")


def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def recipe(name):
    # Preserve actual air, feathers, foliage, crawling, music, parchment
    # and, unconditionally, every Strider recording.
    if name.startswith("pale_owl_hoot_"):
        return "owl_voice"
    if name.startswith("soul_shield_swing_"):
        return None
    if name.startswith("soul_shield_"):
        if re.fullmatch(r"soul_shield_charge_[1-4]", name):
            return "charge"
        if name in ("soul_shield_block_1", "soul_shield_block_2",
                    "soul_shield_bastion", "soul_shield_riposte",
                    "soul_shield_raise", "soul_shield_ram_hit",
                    "soul_shield_ram_start", "soul_shield_break",
                    "soul_shield_clap_prepare"):
            return "soul_contact"
        return "soul_harmony"
    if name.startswith(("pale_crossbow_", "pale_spear_", "pale_shield_", "pale_soldier_")):
        if re.search(r"_(ambient|hurt|death)_\d+$", name):
            return "soldier_voice"
        if re.search(r"_(dodge|thrust|bash_swing)_\d+$", name):
            return "air_gesture"
        return "physical"
    return None


def rms(x):
    return float(np.sqrt(np.mean(x*x)))


class Sound:
    def __init__(self, n, sr, rng):
        self.sr, self.n, self.rng = sr, n, rng
        self.t = np.arange(n) / sr
        self.x = np.zeros(n)

    def modal(self, at, freq, decay, strength=1., attack=.002, bend=0.):
        t = self.t-at
        on = t >= 0
        u = t[on]
        if not len(u): return
        env = (1-np.exp(-u/max(attack, .0001)))*np.exp(-u/max(decay, .001))
        phase = 2*np.pi*(freq*u + bend*decay*(1-np.exp(-u/decay)))
        self.x[on] += strength*env*np.sin(phase)

    def strike(self, at, modes, strength=1.):
        for f,d,a in modes:
            self.modal(at, f*self.rng.uniform(.96,1.04), d, a*strength)

    def click(self, at, strength=1., hard=True):
        # Impulse-like contact, not a broadband loop.
        modes = [(620,.013,.65),(1470,.009,.32),(2910,.004,.2)] if hard else \
                [(130,.025,.7),(360,.013,.43),(710,.007,.18)]
        self.strike(at,modes,strength)

    def air(self, at, length, strength=1., center=600):
        # Air belongs to a movement, never to hits, voices, or magic.
        duration = max(.025,length)
        u = (self.t-at)/duration
        mask = (u>=0)&(u<=1)
        if mask.sum()<16: return
        noise = self.rng.standard_normal(self.n)
        b = signal.butter(2,[max(90,center*.32),min(self.sr*.44,center*2.2)],
                          btype="bandpass",fs=self.sr,output="sos")
        z = signal.sosfilt(b,noise)
        self.x[mask] += strength*z[mask]*np.sin(np.pi*u[mask])**2.8


WOOD = [(105,.085,1.),(245,.049,.42),(430,.032,.28),(760,.018,.15)]
IRON = [(162,.071,.8),(427,.064,.62),(1060,.040,.28),(2310,.022,.15)]
STONE = [(81,.085,1.),(143,.054,.52),(298,.032,.26),(547,.017,.16)]
FLESH = [(71,.057,1.),(126,.035,.57),(249,.018,.3)]
BELL = [(196,.12,.72),(392,.19,.45),(588,.14,.21),(785,.12,.12)]


def tonal(x,sr, floor=.035, time_width=11):
    """Frequency-selective harmonic mask. Retains original pitch and phrasing.
    No blanket low-pass; smooth mask to avoid musical-noise speckles."""
    nper = min(2048, max(256,2**int(np.log2(max(256,len(x)//3)))))
    f,t,z=signal.stft(x,fs=sr,nperseg=nper,noverlap=nper*3//4)
    mag=np.abs(z)
    h=ndimage.median_filter(mag,size=(1,time_width),mode="nearest")
    p=ndimage.median_filter(mag,size=(15,1),mode="nearest")
    mask=(h*h)/(h*h+2.4*p*p+1e-15)
    mask=ndimage.gaussian_filter(mask,sigma=(.6,.7))
    # Remove the diffuse bed, retain actual formants/harmonics and low body.
    _,y=signal.istft(z*(floor+(1-floor)*mask),fs=sr,nperseg=nper,noverlap=nper*3//4)
    return np.pad(y[:len(x)],(0,max(0,len(x)-len(y))))


def physical(name,s):
    D=s.n/s.sr
    # Crossbow mechanisms: distinct individual joints, spring and string.
    if "reload_bolt" in name:
        s.strike(.01,WOOD,.48);s.click(.032,.78);s.click(min(.09,D*.55),.38)
    elif "reload_draw" in name:
        for j in range(5):
            p=D*(.13+.115*j);s.click(p,.20+.10*j)
            s.modal(p,230+48*j,.034,.32)
        s.strike(D*.73,WOOD,.4)
    elif "reload_latch" in name:
        s.click(.012,1.1);s.strike(.021,IRON,.65);s.click(D*.30,.27)
    elif "_shoot_" in name:
        s.modal(.005,138,.045,1.,bend=-35)
        s.modal(.006,410,.025,.65,bend=-70)
        s.modal(.009,835,.017,.4)
        s.strike(.013,WOOD,.73);s.click(.005,.55)
    elif "bolt_impact" in name:
        s.strike(.004,WOOD,1.);s.click(.003,.43);s.modal(.01,985,.034,.27)
    elif "charge_break_soft" in name:
        s.strike(.005,WOOD,1.)
        for p,a in [(0.045,.52),(.095,.31),(.15,.18)]:
            s.click(p,a,False)
    elif "charge_wall" in name:
        s.strike(.006,STONE,1.);s.strike(.018,IRON,.6)
        s.strike(D*.27,STONE,.42)
    elif "charge_prepare" in name:
        # Heavy brace / weight-shift sequence, not an atmospheric warning hiss.
        s.strike(.035,IRON,.55);s.click(D*.25,.45)
        s.strike(D*.48,WOOD,.62);s.strike(D*.72,STONE,.79)
        s.modal(D*.72,112,.105,.4)
    elif "charge_start" in name:
        s.strike(.006,STONE,.95);s.strike(.04,IRON,.55)
        s.air(.025,min(.14,D*.55),.10,340)
    elif "charge_hit" in name or "bash_hit" in name:
        s.strike(.006,STONE,1.);s.strike(.012,IRON,.64)
        s.click(.004,.23,False)
    elif "deflect" in name:
        s.click(.003,.75);s.strike(.006,IRON,.82)
        s.modal(.008,1750,.052,.36,bend=30)
    elif "_block_" in name:
        s.strike(.004,IRON,.83);s.strike(.009,WOOD,.84)
        s.click(.005,.35)
    elif "_disabled_" in name:
        s.strike(.005,IRON,.9);s.click(.035,.65)
        s.strike(D*.27,WOOD,.66);s.click(D*.54,.25)
    elif "_raise_" in name:
        s.strike(.01,WOOD,.45);s.click(D*.30,.55)
        s.strike(D*.54,IRON,.46)
    elif "_stun_" in name:
        s.strike(.015,IRON,.7);s.modal(.04,480,.15,.38)
        s.strike(D*.36,STONE,.42)
    elif "_step_" in name:
        # Soles and armor: little distinct weight, no long scrape.
        modes=STONE if "shield" in name else WOOD if "crossbow" in name else FLESH
        s.strike(.004,modes,.85)
        s.strike(min(.06,D*.25),IRON,.30 if "shield" in name else .12)
    elif "spear_hit" in name:
        s.strike(.003,FLESH,.90);s.click(.003,.75)
        s.modal(.006,1140,.023,.28)
    else:
        raise ValueError("unhandled physical cue: "+name)
    return s.x


def soul_contact(name,s):
    D=s.n/s.sr
    if "raise" in name or "prepare" in name:
        s.modal(.01,196,.07,.65,attack=.025)
        s.modal(D*.22,294,.08,.40,attack=.018)
    elif "break" in name:
        s.strike(.007,STONE,.85)
        for j in range(4):
            s.modal(D*(.10+.12*j),[510,431,354,267][j],.045,.39/(j+1)**.4)
    elif "ram_start" in name:
        s.modal(.01,76,.13,1.,attack=.017,bend=-15)
        s.strike(D*.18,WOOD,.46)
    elif "ram_hit" in name or "bastion" in name:
        s.strike(.004,STONE,1.);s.strike(.009,BELL,.34)
    elif "riposte" in name:
        s.click(.006,.42);s.strike(.015,BELL,.84)
        s.modal(D*.18,587,.09,.30)
    elif "block" in name:
        s.strike(.005,WOOD,.74);s.strike(.008,BELL,.55)
        s.click(.005,.32,False)
    else:
        raise ValueError(name)
    return s.x


def render(name,kind,x,sr,rng):
    s=Sound(len(x),sr,rng)
    D=len(x)/sr
    if kind=="physical": return physical(name,s)
    if kind=="soul_contact":
        # Keep a faint original harmonic signature only, not the noise bed.
        y=soul_contact(name,s)
        h=tonal(x,sr,.015)
        return y/max(rms(y),1e-8)+.14*h/max(rms(h),1e-8)
    if kind=="air_gesture":
        center=520 if "dodge" in name else 940 if "thrust" in name else 345
        s.air(D*.05,D*.66,.7,center)
        if "bash_swing" in name: s.modal(D*.12,172,.035,.21)
        elif "thrust" in name: s.modal(D*.08,425,.024,.12)
        else: s.strike(D*.67,FLESH,.22)
        return s.x
    if kind=="charge":
        # Preserve the original note: only remove the unpitched bed.
        return tonal(x,sr,.015,7)
    if kind=="soul_harmony":
        # Keep the approved compositions, arrangements and original dynamics.
        # A short event-specific contact is retained where gameplay calls for it.
        h=tonal(x,sr,.025,9)
        if any(w in name for w in ("impact","shockwave","burst","clap","avalanche","perfect")):
            s.strike(.005,STONE if "avalanche" in name else BELL,.48)
            h=h/max(rms(h),1e-8)+.17*s.x/max(rms(s.x),1e-8)
        return h
    if kind=="soldier_voice":
        # Preserve the voice/formants; no replacement by a rustling breath synth.
        return tonal(x,sr,.12,7)
    if kind=="owl_voice":
        return tonal(x,sr,.06,13)
    raise ValueError(kind)


def match_level(y,x,sr):
    y=np.asarray(y,dtype=np.float64)
    y-=np.mean(y)
    n=min(len(y)//8,max(2,int(sr*.004)))
    y[:n]*=np.linspace(0,1,n);y[-n:]*=np.linspace(1,0,n)
    if not np.isfinite(y).all() or rms(y)<1e-8:
        raise ValueError("empty/invalid synthesized audio")
    old_rms=rms(x)
    old_peak=max(float(np.max(np.abs(x))),.01)
    y*=min(old_rms/rms(y), min(.82,old_peak*1.12)/max(np.max(np.abs(y)),1e-8))
    return y


def main():
    paths=sorted(ROOT.glob("*.ogg"))
    if not paths: raise RuntimeError("no source OGG")
    frozen={p.name:digest(p) for p in paths if not recipe(p.stem)}
    chosen=[p for p in paths if recipe(p.stem)]
    if len(chosen)!=81:
        raise RuntimeError(f"expected 81 targeted sources, found {len(chosen)}; review inventory")
    results=[]
    with tempfile.TemporaryDirectory() as tmp:
        tmp=Path(tmp)
        for p in chosen:
            x,sr=sf.read(p,dtype="float64")
            if x.ndim!=1: raise ValueError("not mono: "+p.name)
            rng=np.random.default_rng(int.from_bytes(hashlib.sha256((SEED+p.name).encode()).digest()[:8],"big"))
            kind=recipe(p.stem)
            y=match_level(render(p.stem,kind,x,sr,rng),x,sr)
            out=tmp/p.name
            sf.write(out,y,sr,format="OGG",subtype="VORBIS")
            z,zsr=sf.read(out,dtype="float64")
            if len(z)!=len(x) or zsr!=sr or z.ndim!=1 or not np.isfinite(z).all() or np.max(np.abs(z))>=.99:
                raise ValueError("encoded validation failed: "+p.name)
            if rms(z)<rms(x)*.2:
                raise ValueError("excessive level reduction: "+p.name)
            results.append(dict(file=p.name,recipe=kind,frames=len(x),rate=sr,
                             rms_before=round(20*np.log10(rms(x)),2),
                             rms_after=round(20*np.log10(rms(z)),2),
                             peak_after=round(20*np.log10(np.max(np.abs(z))),2),
                             before=digest(p),after=digest(out)))
        # No replacements until all candidates pass.
        for p in chosen:
            p.write_bytes((tmp/p.name).read_bytes())
    assert all(digest(ROOT/name)==sha for name,sha in frozen.items())
    REPORT.parent.mkdir(parents=True,exist_ok=True)
    REPORT.write_text(json.dumps({"changed":results,"untouched":frozen},indent=2)+"\n")
    print("Validated and replaced",len(results),"sources; preserved",len(frozen),"byte-for-byte.")
    for kind in sorted({r["recipe"] for r in results}):
        print(kind, sum(r["recipe"]==kind for r in results))


if __name__=="__main__":
    main()
