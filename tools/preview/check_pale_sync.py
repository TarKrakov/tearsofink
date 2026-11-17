"""Actual timeline checks for paired steps 3/4, including captured interruption blends."""
from pathlib import Path
from tl import *
a,model,fp,tp,grip=load('pale_linear_greatsword');hilt,tip=axis(model,fp,grip)
combo=json.load(open(COMBO+'/pale_linear_greatsword.json'))
steps=[(s.get('anim')or None,s.get('off_anim')or None,s['duration']/20)for s in combo['dual_steps']]
S=np.diag([-1.,1.,1.]);maxima=np.zeros(3);samples={}
def inspect(at,first,carried):
    if not .8<=at<1.6:return
    assert all(k in first and k in carried for k in(1,-1))
    f=[apply4(first[k],np.array([hilt,tip])/16)for k in(1,-1)]
    t=[apply4(carried[k],np.array([hilt,tip])/16)for k in(1,-1)]
    axis=[p[1]-p[0]for p in t];axis=[d/np.linalg.norm(d)for d in axis]
    errors=[abs(f[0]@S-f[1]).max(),np.linalg.norm(S@axis[0]-axis[1]),np.linalg.norm(S@t[0][0]-t[1][0])]
    maxima[:]=np.maximum(maxima,errors)
    samples[round(at*120)]={k:(first[k],carried[k])for k in(1,-1)}
build('pale_linear_greatsword',steps,True,'unused.gif',fps=120,observer=inspect,render_frames=False)
assert maxima[0]<2e-4 and maxima[1]<2e-4,('paired phase/axis drift',maxima)
# The residual TP hilt difference is the author's separate left/right Display,
# about one model pixel, not a changing phase or extra item translation.
assert maxima[2]<.08,('grip separation',maxima)
edge_max=[0.,0.]
for impact in(1.,1.4):
    index=round(impact*120)
    for sign in(1,-1):
        for view in(0,1):
            before=samples[index-1][sign][view];after=samples[index+1][sign][view];now=samples[index][sign][view]
            velocity=apply4(after,np.array([tip])/16)[0]-apply4(before,np.array([tip])/16)[0]
            normal=now[:3,:3]@np.array([0.,0,1.]);normal/=np.linalg.norm(normal)
            ratio=abs(normal@velocity)/np.linalg.norm(velocity);edge_max[view]=max(edge_max[view],ratio)
            assert ratio<.12,('paired slam hits broadside',impact,sign,view,ratio)
            if view==1: assert abs(normal[1])<.06,('F5 blade face points at the ground',impact,sign,normal)
print('PASS: actual steps 3/4 at120Hz: mirrored FP grip/tip %.8f blocks; F5 axis %.8f; authored grip difference %.5f blocks'%tuple(maxima))
print('PASS: edge-first downward impacts in both hands; normal/velocity FP %.4f / F5 %.4f'%tuple(edge_max))
# The extra edge turn must not move the existing grip, tip or carrying arm.
reference=json.load(open(Path(__file__).parent/'references/pale_pair_slam_before_edge.json'))
main=a['animations']['axe_dual_slam'];source=a['animations'][main['third']['source']]
unchanged=np.zeros(3)
for at in np.linspace(0,.3,121):
    r,p=sample(main,at);r0,p0=sample(reference['main'],at)
    q=apply4(fp_item_m(fp,r,p,grip,1),np.array([hilt,tip])/16);q0=apply4(fp_item_m(fp,r0,p0,grip,1),np.array([hilt,tip])/16)
    unchanged[0]=max(unchanged[0],abs(q-q0).max())
    x,y,W,_=solve4(model['display'],1,source,at,a['third'],main['third'],grip,hilt,tip)
    x0,y0,W0,_=solve4(model['display'],1,reference['source'],at,a['third'],reference['main']['third'],grip,hilt,tip)
    unchanged[1]=max(unchanged[1],abs(x-x0),abs(y-y0))
    q=apply4(tp_item_m(WORLD@part_m([-5,2,0],x,y,0),tp,grip,W,1),np.array([hilt,tip])/16)
    q0=apply4(tp_item_m(WORLD@part_m([-5,2,0],x0,y0,0),tp,grip,W0,1),np.array([hilt,tip])/16)
    unchanged[2]=max(unchanged[2],abs(q-q0).max())
# FP was deliberately restaged after the user's screen-space feedback.
# The accepted F5 path and arm are still frozen against this reference.
assert unchanged[1]<.002 and unchanged[2]<.002,('F5 pose changed',unchanged)
print('PASS: accepted F5 arm angles / path preserved; max errors',unchanged[1:].tolist())
