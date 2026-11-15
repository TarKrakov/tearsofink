import json, sys, math
sys.path.insert(0,'.')
from tl import *

def rot_between(a, b):
    a = a/np.linalg.norm(a); b = b/np.linalg.norm(b)
    v = np.cross(a, b); c = a@b
    if np.linalg.norm(v) < 1e-8: return np.eye(3)
    K = np.array([[0,-v[2],v[1]],[v[2],0,-v[0]],[-v[1],v[0],0]])
    return np.eye(3)+K+K@K*(1/(1+c))

def euler_xyz(R):
    b = math.asin(max(-1,min(1,R[0,2])))
    a = math.atan2(-R[1,2], R[2,2]); c = math.atan2(-R[0,1], R[0,0])
    return np.degrees([a,b,c])

def frac(R, f):
    ang = math.acos(max(-1,min(1,(np.trace(R)-1)/2)))
    if ang < 1e-6: return np.eye(3)
    ax = np.array([R[2,1]-R[1,2], R[0,2]-R[2,0], R[1,0]-R[0,1]])/(2*math.sin(ang))
    K = np.array([[0,-ax[2],ax[1]],[ax[2],0,-ax[0]],[-ax[1],ax[0],0]]); a = ang*f
    return np.eye(3)+math.sin(a)*K+(1-math.cos(a))*K@K

D = np.array([0.35, 0.55, 0.75])
def design(arch, fwd=np.array([-0.05, 0.12, -0.45]), wind=np.array([0.04,-0.05,0.12])):
    a, model, fp, tp, grip = load(arch)
    disp = model['display']; fpR = disp['firstperson_righthand']
    handle, tip = axis(model, fpR, grip)
    h0 = gp(handle, fpR, 1); t0 = gp(tip, fpR, 1)
    G = gp(grip, fpR, 1) if grip is not None else None
    R = rot_between(t0-h0, D)
    keys = [{"t":0,"rotation":[0,0,0],"translation":[0,0,0],"ease":"linear"}]
    for t, f, off in ((0.07, 0.45, wind), (0.15, 1.0, fwd)):
        Rf = frac(R, f)
        if grip is not None:
            pos = off*16
        else:
            # рукоять не уезжает: o + Rf*h0 = HAND + h0 + off
            pos = (h0 + off - Rf@h0)*16
        keys.append({"t":t,"rotation":[round(x,1) for x in euler_xyz(Rf)],"translation":[round(x,2) for x in pos],"ease":"linear"})
    return {"length":0.5,"hold":0.1,"return":0.22,"impact":0.15,"smooth":True,"keyframes":keys}

if __name__ == '__main__':
    out = {}
    rows=[]
    for arch in ['scimitar','talwar','dussak']:
        clip = design(arch); out[arch]=clip
        a, model, fp, tp, grip = load(arch)
        cells=[cell(arch,'hilt',t,model,model['display'],grip,clip,False,TUNE) for t in [0,0.07,0.15,0.25,0.35,0.45]]
        img = Image.new('RGB',(1200,550))
        for i,c in enumerate(cells): img.paste(c,(i*200,0))
        rows.append(img)
    o=Image.new('RGB',(1200,1650))
    for i,im in enumerate(rows): o.paste(im,(0,i*550))
    o.save('hilt.png'); json.dump(out, open('hilt_clips.json','w'), indent=1)
    print(json.dumps(out)[:1500])
