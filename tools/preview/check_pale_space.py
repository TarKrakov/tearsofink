"""Spatial checks motivated by the author's screenshots: head/torso and separate paired lanes.
This is a geometric regression check, not artistic approval.
"""
import numpy as np
from run import load
from third import *

def box_hits(polygons, lower, upper):
    p=np.concatenate([v for v,_ in polygons]);q=np.concatenate([np.roll(v,-1,axis=0) for v,_ in polygons]);d=q-p
    lo=np.array(lower)+1e-5;hi=np.array(upper)-1e-5
    parallel=np.abs(d)<1e-10
    outside=np.any(parallel & ((p<lo)|(p>hi)),axis=1)
    safe=np.where(parallel,1,d)
    x=(lo-p)/safe;y=(hi-p)/safe
    near=np.where(parallel,-np.inf,np.minimum(x,y));far=np.where(parallel,np.inf,np.maximum(x,y))
    enter=np.maximum(0,near.max(axis=1));leave=np.minimum(1,far.min(axis=1))
    return np.any((enter<leave)&~outside)

def check():
    a,m,fp,_,g=load('pale_linear_greatsword');h,tip=axis(m,fp,g);anims=a['animations'];minimum=10
    for name in ('dual_attack1','dual_attack2','dual_attack3','dual_parry','dual_repel','dual_rush','dual_charge','dual_leap','dual_slam'):
        length=anims[name]['keyframes'][-1]['t']
        for time in np.arange(0,end_time(anims[name])+1/120,1/60):
            lanes=[]
            for sign,suffix in ((1,''),(-1,'_off')):
                c=anims[name+suffix];fix=dict(c.get('third',{}))
                if fix.get('wrist_source'):fix['_wrist_clip']=anims[fix['wrist_source']]
                original=c;source=fix.get('source');c=anims[source] if source else c
                disp=m['display']['thirdperson_'+('right' if sign==1 else 'left')+'hand']
                from tl import hand_pose
                arm,W,_,_,_,_,_,_=hand_pose('pale_linear_greatsword',m,g,original,time,sign,a['third'])
                x,y,z=arm
                matrix=tp_item_m(WORLD@part_m([-sign*5,2,0],x,y,z),disp,h,W,sign)
                quads=item_quads(m,matrix,(255,0,0))
                assert not box_hits(quads,[-.25,1.501,-.25],[.25,2.001,.25]),(name,suffix,time,'head collision')
                assert not box_hits(quads,[-.25,.751,-.125],[.25,1.501,.125]),(name,suffix,time,'torso collision')
                points=apply4(matrix,model_points(m)/16)
                lanes.append(np.min(sign*points[:,0]))
            # A separating plane exists between the entire meshes, not just their tips.
            gap=sum(lanes);minimum=min(gap,minimum)
            assert gap>.02,(name,time,'paired meshes cross their carrying lanes',gap)
    from tl import hand_pose
    for name in ('attack1','attack2','attack3','ability','rush'):
        c=anims[name]
        for at in np.arange(0,end_time(c),1/60):
            arm,W,tp,handle,_,by,_,_=hand_pose('pale_linear_greatsword',m,g,c,at,1,a['third'])
            body=np.clip(by*a['third']['body'],-D(25),D(25))
            matrix=tp_item_m(WORLD@part_m([-5*math.cos(body),2,5*math.sin(body)],*arm),tp,handle,W,1)
            quads=item_quads(m,matrix,(255,0,0))
            assert not box_hits(quads,[-.25,1.501,-.25],[.25,2.001,.25]),(name,at,'head collision')
            assert not box_hits(quads,[-.25,.751,-.125],[.25,1.501,.125]),(name,at,'torso collision')
    print(f'PASS: active paired blades clear head/torso and have separate F5 lanes; minimum X gap {minimum:.3f} blocks')
if __name__=='__main__':check()
