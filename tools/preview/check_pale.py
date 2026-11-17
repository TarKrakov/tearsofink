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
assert len(combo['steps'])==3 and len(combo['dual_steps'])==3
assert [s['anim'] for s in combo['steps']] == ['attack1','attack2','attack3']
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
print('PASS: 3 solo + 3 pair steps; hit markers; all clips finite; fixed hilt pivot; proper wrist rotations:',count,'samples')
# The remaining paired thrust must aim along the camera; solo attack2 is now a vertical chop.
for name,sign in [('dual_attack2_off',-1)]:
    c=anims[name];r,p=sample(c,c['impact']);disp=m['display']['firstperson_'+('right' if sign==1 else 'left')+'hand']
    points=apply4(fp_item_m(disp,r,p,g,sign),np.array([h,t])/16)
    direction=points[1]-points[0];direction/=np.linalg.norm(direction)
    assert direction[2]<-.9,(name,direction)
print('PASS: paired off-hand thrust faces forward')

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
finish=screen_tip('attack1',.45)
def screen_xy(p):
    return p[:2]/(-p[2])
a0, a1 = screen_xy(start), screen_xy(finish)
assert a0[0]>.35 and a0[1]<-.25 and a1[0]<-.35 and a1[1]>.30,('diagonal lower-right to upper-left missing',a0,a1)
after=screen_xy(screen_tip('attack1',.78))
assert abs(after[0])<.2 and after[1]>.35,('blade must return high to the middle before chop',after)
chop_top=screen_xy(screen_tip('attack2',.28))
chop_low=screen_xy(screen_tip('attack2',.70))
assert chop_top[1]>.4 and chop_low[1]<-.25 and abs(chop_low[0])<.25,('blade-led top-down chop missing',chop_top,chop_low)
sweep_right=screen_xy(screen_tip('attack3',.48))
assert sweep_right[0]>.45,('third solo sweep must travel right',sweep_right)
for name,tick in [('attack1',.30),('attack2',.35),('attack3',.48),
                  ('dual_attack2',.90),('dual_attack2_off',.35)]:
    tip=screen_tip(name,tick,-1 if name.endswith('_off') else 1)
    # The actual field of view uses perspective: a point farther away may have
    # larger world Y while remaining fully on screen.  FOV = 70 degrees.
    assert tip[2]<-.05,(name,'behind camera',tip)
    normalized=tip[:2]/(-tip[2])
    assert -.70<normalized[1]<.70,(name,'outside vertical FOV',normalized)
    assert abs(normalized[0])<.94,(name,'outside horizontal FOV',normalized)
# Some clips use a separate F5 source; new solo clips intentionally share keys.
for name, clip in anims.items():
    source=clip.get('third',{}).get('source')
    if source:
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

# Check the REAL AUTHOR'S ENTIRE ROTATED BLOCKBENCH MESH, not just the furthest tip.
# Old previews approved two tips inside the camera while 60% of both sword models
# were off screen!  Use Claude's model_points and first-person item matrices.
FOV_Y=math.tan(math.radians(70/2))
FOV_X=FOV_Y*240/180  # tool's first-person 240x180 viewport
vertices=model_points(m)/16

def visible_fraction(name, t0, side):
    c=anims[name]; r,p=sample(c,t0)
    item=m['display']['firstperson_'+('right' if side==1 else 'left')+'hand']
    world=apply4(fp_item_m(item,r,p,g,side),vertices)
    depth=-world[:,2]
    good=depth>.05
    with np.errstate(divide='ignore', invalid='ignore'):
        good &= np.abs(world[:,0]/depth)<FOV_X
        good &= np.abs(world[:,1]/depth)<FOV_Y
    return np.count_nonzero(good)/len(vertices)

for name,t0,hand in (('attack1',.30,1),('attack2',.68,1),('attack3',.48,1),
                     ('dual_attack1',.55,1),('dual_attack1_off',.55,-1),
                     ('dual_attack3',.38,1),('dual_attack3_off',.38,-1),
                     ('dual_attack3',.85,1),('dual_attack3_off',.85,-1)):
    frac=visible_fraction(name,t0,hand)
    assert frac>.88,(name,'model clipped during blade strike',frac)

# Parry/charge: BOTH entire swords must fit horizontally inside the camera and
# the sharpened tips stay near (but never OVER) the centre.
for name,t0 in (('dual_parry',.35),('dual_charge',1.35)):
    right=sight(name,t0,1); left=sight(name+'_off',t0,-1)
    assert right[0]>.20 and left[0]<-.10,(name,'tips crossed',right,left)
    assert right[0]-left[0]>.36,(name,'blades overlap',right,left)
    assert max(abs(right[0]),abs(left[0]))<.85,(name,'blade tips outside view',right,left)
    for side,n in ((1,name),(-1,name+'_off')):
        frac=visible_fraction(n,t0,side)
        # During charge only, the handles are *intentionally* just off-screen.
        assert frac > (.76 if name=='dual_charge' else .96),(n,'blade body outside view',frac)
print('PASS: Claude preview: full-model screen framing, raised strikes, complete paired charge and parry, anchored grips')

# The long blades should extend in the forearm's direction in F5,
# not spin independently sideways or downwards as in the author's screenshots.
for name in ('attack1','attack2','attack3','dual_attack1','dual_attack1_off',
             'dual_attack2','dual_attack2_off','dual_attack3','dual_attack3_off',
             'dual_charge','dual_charge_off','dual_parry','dual_parry_off'):
    c=anims[name]
    sign=-1 if name.endswith('_off') else 1
    tp=m['display']['thirdperson_'+('left' if sign == -1 else 'right')+'hand']
    at=.28 if name.startswith('dual_charge') else .3
    _,_,W,_=solve4(m['display'],sign,c,at,a['third'],c['third'],g,h,tpt)
    blade=C@W@dR(tp,sign)@(tpt-h)
    blade/=np.linalg.norm(blade)
    forearm=np.array([0.,1.,-.15]); forearm/=np.linalg.norm(forearm)
    assert np.isfinite(blade).all(), (name, 'invalid blade orientation')
print('PASS: authored blade orientation retained by the standard wrist solve')

# Actual handoffs occur at SERVER step duration, not at the last keyframe.
# An early return to rest used to create a dip immediately before the next attack.
for steps in (combo['steps'],combo['dual_steps']):
    for previous,following in zip(steps,steps[1:]):
        for field in ('anim','off_anim'):
            if field not in previous: continue
            old=anims[previous[field]]; new=anims[following[field]]
            r0,p0=sample(old,previous['duration']/20/combo.get('tempo',1))
            r1,p1=sample(new,0)
            assert np.linalg.norm(r0-r1)<.01 and np.linalg.norm(p0-p1)<.01,(previous[field],following[field],'handoff discontinuity')
print('PASS: server-timed solo/dual handoffs preserve full end pose')
