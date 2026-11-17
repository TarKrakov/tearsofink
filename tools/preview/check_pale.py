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
assert combo['steps'][2]['late']>=6
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

# Camera framing and readable right-to-left cut (based on Claude's exact item matrices).
def screen_tip(name, t, hand=1):
    c=anims[name]
    rot,pos=sample(c,t)
    side='right' if hand==1 else 'left'
    frame=m['display']['firstperson_'+side+'hand']
    pts=apply4(fp_item_m(frame,rot,pos,g,hand),np.array([h,tpt])/16)
    # fp_item_m already contains the vanilla camera-to-hand translation.
    return pts[1]
tpt=t
start=screen_tip('attack1',0)
finish=screen_tip('attack1',.43)
assert start[0]>.25 and finish[0]<-.15,('right-to-left swing missing',start,finish)
for name,tick in [('attack1',.30),('attack2',.35),('attack3',.75),('attack4',.30),
                  ('dual_attack2',.90),('dual_attack2_off',.35)]:
    tip=screen_tip(name,tick,-1 if name.endswith('_off') else 1)
    # The actual field of view uses perspective: a point farther away may have
    # larger world Y while remaining fully on screen.  FOV = 70 degrees.
    assert tip[2]<-.05,(name,'behind camera',tip)
    normalized=tip[:2]/(-tip[2])
    assert -.70<normalized[1]<.70,(name,'outside vertical FOV',normalized)
    assert abs(normalized[0])<.94,(name,'outside horizontal FOV',normalized)
for name in ('attack1','attack2','attack3','attack4','dual_attack2','dual_attack2_off',
             'dual_attack3','dual_attack3_off'):
    source=anims[name].get('third',{}).get('source')
    assert source in anims,(name,'missing third-person source',source)

# The pilot's third.push once moved blades away from the character's actual wrists.
# Shield-only item translation must never reappear on this sword.
assert a['third'].get('push', 0) == 0
for name,c in anims.items():
    assert not c.get('third', {}).get('push'), (name, 'item-only third-person push detaches the blade')

def sight(name, t, sign=1):
    point=screen_tip(name,t,sign)
    assert point[2]<-.05, (name,t,'clipped behind camera',point)
    return np.array([point[0]/(-point[2]), point[1]/(-point[2])])

# Different first-/third-person animations may be used, but the pivot at the grip stays
# exactly stationary through any wrist rotation.  Check each animation source separately.
for name in ('attack1','attack2','attack3','attack4','dual_attack1','dual_attack1_off',
             'dual_attack2','dual_attack2_off','dual_attack3','dual_attack3_off',
             'dual_charge','dual_charge_off','dual_parry','dual_parry_off'):
    c=anims[name]
    src=c.get('third',{}).get('source')
    if src:
        c=dict(c,keyframes=anims[src]['keyframes'])
    fx=c.get('third',{})
    for sign in (1,-1):
        disp=m['display']['thirdperson_'+('right' if sign==1 else 'left')+'hand']
        for t0 in (0.1, 0.3, 0.55):
            _,_,W,_=solve4(m['display'],sign,c,t0,a['third'],fx,g,h,tpt)
            grip_px=np.array([h])/16
            still=apply4(tp_item_m(np.eye(4),disp,h,None,sign),grip_px)[0]
            moved=apply4(tp_item_m(np.eye(4),disp,h,W,sign),grip_px)[0]
            assert np.linalg.norm(still-moved)<1e-8,(name,sign,'third-person grip drift',still,moved)

# 1. Mirrored slashes travel from the middle to the OUTER screen edges.
r0=sight('dual_attack1',0,1); l0=sight('dual_attack1_off',0,-1)
r1=sight('dual_attack1',.55,1); l1=sight('dual_attack1_off',.55,-1)
assert r0[0]<-.15 and l0[0]>.15,(r0,l0)
assert .55<r1[0]<.95 and -.95<l1[0]<-.55,(r1,l1)

# 2. Separate thrusts follow the first hit position, one tip at a time toward the centre.
assert sight('dual_attack2',0,1)[0]>.55
assert sight('dual_attack2_off',0,-1)[0]<-.55
assert abs(sight('dual_attack2_off',.35,-1)[0])<.35
assert abs(sight('dual_attack2',.9,1)[0])<.35

# 3. Vertical blade-led split: BOTH tips rise near the top and slash down to the toolbar,
#    remaining visible without swapping left/right or crossing the camera centre.
for side,name in ((1,'dual_attack3'),(-1,'dual_attack3_off')):
    top=sight(name,.38,side); low=sight(name,.85,side)
    assert .4<top[1]<.80,(name,'upper sword outside screen',top)
    assert -.64<low[1]<-.25,(name,'lower sword out of frame',low)
    assert top[0]*side > -.1 and low[0]*side > -.1,(name,'blade crossed centre',top,low)

# Parry/charge: keep two entire blades beside each other, with only their TIPS angled
# towards centre; they must not collide on-screen (and never leave their hands).
for name,t0 in (('dual_parry',.35),('dual_charge',.6)):
    right=sight(name,t0,1); left=sight(name+'_off',t0,-1)
    assert right[0]>.20 and left[0]<-.18,(name,'blades overlap',right,left)
    assert right[0]-left[0]>.45,(name,'blades too close',right,left)
    assert max(abs(right[0]),abs(left[0]))<.85,(name,'blades outside view',right,left)
print('PASS: Claude preview: raised solo attacks, mirrored dual combos, charge/parry gap, anchored grips')
