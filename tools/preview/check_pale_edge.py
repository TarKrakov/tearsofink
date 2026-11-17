"""Check the actual cutting plane against velocity around each strike, FP and F5."""
import numpy as np
from tl import *
a,model,fp,tp,grip=load('pale_linear_greatsword');hilt,tip=axis(model,fp,grip)
maximum=[0.,0.]
for name in ('attack1','attack2','attack3','dual_attack1','dual_attack1_off','dual_attack3','dual_attack3_off'):
    c=a['animations'][name];sign=-1 if name.endswith('_off') else 1
    display=model['display']['firstperson_'+('left' if sign==-1 else 'right')+'hand']
    def matrices(time):
        rot,pos=sample(c,time);first=fp_item_m(display,rot,pos,grip,sign)
        arm,W,tp,h,_,by,_,_=hand_pose('pale_linear_greatsword',model,grip,c,time,sign,a['third'])
        yaw=np.clip(by*a['third']['body'] if not name.startswith('dual') else 0,-D(25),D(25))
        carried=tp_item_m(WORLD@part_m([-sign*5*math.cos(yaw),2,sign*5*math.sin(yaw)],*arm),tp,h,W,sign)
        return first,carried
    for at in np.linspace(c['impact']-.025,c['impact']+.025,7):
        before=matrices(at-.003);after=matrices(at+.003);now=matrices(at)
        for view in(0,1):
            velocity=apply4(after[view],np.array([tip])/16)[0]-apply4(before[view],np.array([tip])/16)[0]
            normal=now[view][:3,:3]@np.array([0.,0,1]);normal/=np.linalg.norm(normal)
            broadside=abs(normal@velocity)/np.linalg.norm(velocity)
            grip_velocity=apply4(after[view],np.array([grip])/16)[0]-apply4(before[view],np.array([grip])/16)[0]
            assert grip_velocity@velocity>0,(name,at,view,'hand moves against the stroke')
            maximum[view]=max(maximum[view],broadside)
            assert broadside<(.24 if view==0 else .16),(name,at,view,'cutting broadside',broadside)
print('PASS: cutting planes follow the impact stroke, max normal/velocity component FP %.3f / F5 %.3f'%tuple(maximum))
