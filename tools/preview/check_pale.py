"""Data/geometry invariants for the pilot, independent of screenshots."""
import json,sys,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
import numpy as np
sys.path.insert(0,str(Path(__file__).resolve().parent))
from run import load
from third import *
a,m,fp,tp,g=load('pale_linear_greatsword'); anims=a['animations']
combo=json.load(open(ROOT/'src/main/resources/data/tears-of-ink/weapon_combos/pale_linear_greatsword.json'))
assert len(combo['steps'])==4 and len(combo['dual_steps'])==3
assert combo['steps'][2]['late']==6
assert combo['dual_steps'][1]['second_hit'] > combo['dual_steps'][1]['hit']
for st in combo['steps']+combo['dual_steps']:
    c=anims[st['off_anim']] if st['anim']=='dual_attack2' else anims[st['anim']]
    assert abs(st['hit']/20-c['impact'])<=.031,(st,c['impact'])
    assert 0 < st['hit'] <= st['duration']
    if st.get('second_hit'):
        assert st['hit'] < st['second_hit'] < st['duration']
    assert st.get('off_anim',st['anim']) in anims
h,t=axis(m,fp,g)
count=0
for name,c in anims.items():
    assert all(x['t']<y['t'] for x,y in zip(c['keyframes'],c['keyframes'][1:])),name
    for time in np.linspace(0,end_time(c)-1e-7,80):
        rot,pos=sample(c,time)
        assert np.all(np.isfinite(rot)) and np.all(np.isfinite(pos)),name
        for sign,side in ((1,'right'),(-1,'left')):
            disp=m['display']['firstperson_'+side+'hand']
            matrix=fp_item_m(disp,rot,pos,g,sign)
            actual=apply4(matrix,np.array([g])/16)[0]
            expected=np.array([sign*.56,-.52,-.72])+np.array([sign*pos[0],pos[1],pos[2]])/16+gp(g,disp,sign)
            assert np.max(np.abs(actual-expected))<1e-10,(name,time,actual,expected)
            x,y,W,P=solve4(m['display'],sign,c,time,a['third'],c.get('third',{}),g,h,t)
            assert np.isfinite([x,y]).all() and np.isfinite(W).all()
            assert abs(np.linalg.det(W)-1)<1e-9
        count+=1
print('PASS: 4 solo + 3 pair steps; hit markers; all clips finite; fixed hilt pivot; proper wrist rotations:',count,'samples')
# The actual first/third-person transforms must aim each thrust along the camera centre.
for name,sign in [('attack2',1),('dual_attack2_off',-1)]:
    c=anims[name];r,p=sample(c,c['impact']);disp=m['display']['firstperson_'+('right' if sign==1 else 'left')+'hand']
    points=apply4(fp_item_m(disp,r,p,g,sign),np.array([h,t])/16)
    direction=points[1]-points[0];direction/=np.linalg.norm(direction)
    assert direction[2]<-.9,(name,direction)
print('PASS: main and off thrusts face forward')
