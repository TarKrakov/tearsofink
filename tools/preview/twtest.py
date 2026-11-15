from pair import *
import json, pair, sys
a, model, fp0, tp0, grip = load('talwar')
def K(t,r,tr): return {"t":t,"rotation":r,"translation":tr,"ease":"linear"}
def run(clipname, clip, times, out, fix=None, single=False):
    a['animations'][clipname]=clip
    orig=pair.load
    pair.load=lambda arch: (a, model, fp0, tp0, grip) if arch=='talwar' else orig(arch)
    if single:
        imgs=[]
        x=[cell('talwar',clipname,t,model,model['display'],grip,clip,False,TUNE) for t in times]
        img=Image.new('RGB',(200*len(x),550))
        for i,c in enumerate(x): img.paste(c,(i*200,0))
        img.save(out); pair.load=orig; return img
    x=pair.pair('talwar',clipname,clipname,times,fix=fix)
    pair.load=orig
    x.save(out); return x
