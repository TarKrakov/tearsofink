"""Rebuild D-026 from physical poses for the updated authored Display (no old keys).
Run from the repository root. Changes animation JSON only; never changes models.
F5 sources control shoulder deltas; wrist sources set edge roll about the blade.
"""
import json
from pathlib import Path
from blade_pose import BladePose
from third import sample

PATH=Path(__file__).resolve().parents[2]/'src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword.json'
p=BladePose('pale_linear_greatsword');root=p.anims;anims=root['animations']
if root.get('lmb_source')=='axe':
    raise SystemExit('D-026 is historical. Current author request: use pale_axe_transfer.py.')
# Metadata is retained for durations/hold/return and event markers, never old poses.
def zero(t=0): return dict(t=t,rotation=[0,0,0],translation=[0,0,0],ease='ease_in_out')
def pose(t,d,h,edge=(1,0,0),left=False,ease='ease_in_out'): return p.key(t,d,edge,h,left,ease)
def make(name,keys,arms,rolls=None,impact=None):
    old=anims[name];c={k:old[k] for k in ('length','hold','return','impact') if k in old}
    if impact is not None:c['impact']=impact
    c.update(smooth='tight',rotation_mode='slerp',keyframes=keys)
    src=name+'_shoulder';c['third']=dict(source=src,arm_space=True)
    # Shoulder channels are modest Euler deltas, not item orientations.
    source={k:c[k] for k in ('length','hold','return')};source.update(smooth='tight',keyframes=[dict(t=t,rotation=r,ease=e) for t,r,e in arms])
    anims[src]=source
    if rolls:
        wrist=name+'_edge';c['third']['wrist_source']=wrist
        anims[wrist]={**{k:c[k] for k in ('length','hold','return')},'smooth':'tight','keyframes':[dict(t=t,rotation=[angle,0,0],ease='ease_in_out') for t,angle in rolls]}
    anims[name]=c

def arm(t,x=0,y=0,z=0,e='ease_in_out'):return t,[x,y,z],e
# Solo: lower-right preparation, fast diagonal, an upper arc to the next chop.
upper=pose(.84,(0,.68,-.73),(.14,-.23,-.9),(-.7,-.7,0))
low=pose(1.17,(0,-.60,-.8),(.14,-.42,-.92),(0,-1,0))
make('attack1',[zero(),pose(.27,(.63,-.22,-.74),(.40,-.38,-.76),(-.82,.58,.08)),pose(.63,(-.57,.63,-.53),(-.03,-.23,-.9),(-.82,.58,.08)),upper],
 [arm(0),arm(.27,15,0,40),arm(.63,-75,0,40),arm(.84,-78,0)],[(0,0),(.27,-35),(.63,-35),(.84,0)])
anims['attack1']['hold']=.3
make('attack2',[dict(upper,t=0),pose(.38,(0,.83,-.55),(.14,-.18,-.86),(0,-1,0)),pose(.53,(0,.83,-.55),(.14,-.18,-.86),(0,-1,0)),dict(low,t=.87),low],
 [arm(0,-78),arm(.38,-100),arm(.53,-100),arm(.87,28),arm(1.17,24)],[(0,0),(1.17,0)])
make('attack3',[dict(low,t=0),pose(.23,(.1,-.62,-.78),(.18,-.43,-.94),(.85,.53,0)),pose(.74,(.76,-.16,-.63),(.45,-.40,-.93),(.85,.53,0)),zero(1.04)],
 [arm(0,24),arm(.23,25,8),arm(.74,12,55),arm(1.04)],[(0,0),(.23,-65),(.5,-65),(1.04,0)],impact=.50)
# Each hand has its own Display, baseline and independently solved relative keys.
# Semantic symmetry is only a target direction, never a second display reflection.
for left in (False,True):
    suffix='_off' if left else ''; sign=-1 if left else 1
    outer=pose(.79,(.85,.08,-.52),(.50,-.40,-1.05),(1,0,0),left)
    make('dual_attack1'+suffix,[zero(),pose(.29,(-.10,.10,-1),(.24,-.4,-.82),(1,0,0),left,ease='ease_in'),pose(.55,(.58,.08,-.81),(.40,-.40,-1.01),(1,0,0),left,ease='linear'),outer],
         [arm(0),arm(.29,1,-8,e='ease_in'),arm(.55,1,43,e='linear'),arm(.79,1,62)],[(0,0),(.29,-sign*76),(.79,-sign*76)])
    anims['dual_attack1'+suffix]['hold']=.4
    anims[anims['dual_attack1'+suffix]['third']['source']]['hold']=.4
    # Left stabs on tick 7, right on tick18. Retraction is slower than extension.
    hit=.35 if left else .90
    keys=[dict(outer,t=0),pose(hit-.14,(.02,.06,-1),(.28,-.40,-.78),(1,0,0),left,ease='ease_in'),pose(hit,(-.15,.08,-1),(.19,-.39,-1.26),(1,0,0),left,ease='linear'),pose(hit+.18,(-.1,.12,-1),(.23,-.39,-.96),(1,0,0),left),pose(1.28,(.02,.22,-.98),(.28,-.40,-.87),(1,0,0),left)]
    make('dual_attack2'+suffix,keys,[arm(0,1,62),arm(hit-.14,-3,8,e='ease_in'),arm(hit,-14,-3,e='linear'),arm(hit+.18,0,15),arm(1.28,-10,8)],[(0,-sign*76),(hit-.14,-sign*15),(hit,-sign*15),(1.28,0)])
    start=dict(keys[-1],t=0)
    make('dual_attack3'+suffix,[start,pose(.42,(.08,.74,-.67),(.27,-.24,-.87),(0,-1,0),left),pose(.82,(.08,-.55,-.83),(.28,-.43,-.98),(0,-1,0),left),pose(1.05,(.08,-.61,-.79),(.28,-.44,-1.03),(0,-1,0),left),zero(1.28)],
         [arm(0,-10,8),arm(.42,-95,8),arm(.82,27,8),arm(1.05,31,8),arm(1.28)],[(0,0),(1.28,0)])
    # Slow charge: both grips just outside the view, tips inward with a clear gap.
    charge=pose(1.35,(-.67,.13,-.73),(.91,-.40,-.98),(-1,0,0),left)
    make('dual_charge'+suffix,[zero(),pose(.55,(-.32,.27,-.91),(.66,-.40,-.93),(-1,0,0),left),charge],
         [arm(0),arm(.55,-4,20),arm(1.35,-5,40)],[(0,0),(1.35,0)])
    guard=pose(.25,(.08,.68,-.73),(.31,-.41,-.92),(-1,0,0),left)
    make('dual_parry'+suffix,[zero(),guard,dict(guard,t=.55)],
         [arm(0),arm(.25,-35,8),arm(.55,-35,8)],[(0,0),(.55,0)])
    spread=pose(.24,(.72,.18,-.67),(.38,-.38,-.93),(1,0,0),left)
    make('dual_repel'+suffix,[dict(guard,t=0),pose(.12,(.45,.2,-.87),(.34,-.38,-1.04),(1,0,0),left,ease='linear'),spread],
         [arm(0,-35,8),arm(.12,-15,40,e='linear'),arm(.24,-5,55)],[(0,0),(.12,-sign*60),(.24,-sign*60)])
    raised=pose(.35,(.1,.75,-.65),(.29,-.24,-.89),(0,-1,0),left)
    make('dual_leap'+suffix,[zero(),raised,dict(raised,t=.70)],
         [arm(0),arm(.35,-95,10),arm(.70,-95,10)],[(0,0),(.7,0)])
    make('dual_slam'+suffix,[dict(raised,t=0),pose(.22,(.10,-.50,-.86),(.28,-.42,-1.03),(0,-1,0),left,ease='linear'),pose(.40,(.1,-.63,-.77),(.28,-.44,-.99),(0,-1,0),left),zero(.65)],
         [arm(0,-95,10),arm(.22,22,10,e='linear'),arm(.40,32,10),arm(.65)],[(0,0),(.65,0)])
    for name,dual in [('ability',False),('rush',False),('dual_rush',True)]:
        nm=name+suffix
        if nm not in anims:continue
        direction=(.05,.15,-1) if 'rush' in name else (.63,.22,-.74)
        prepare=zero() if not dual else dict(charge,t=0)
        hit=.12 if 'rush' in name else (.10 if left else .18)
        k=pose(hit,direction,(.24,-.40,-1.22 if 'rush' in name else -.95),(1,0,0),left,ease='linear')
        make(nm,[prepare,k,dict(k,t=.35),zero(.72 if 'rush' in name else .58)],
             [arm(0, -5 if dual else 0,40 if dual else 0),arm(hit,-12,0 if 'rush' in name else 42,e='linear'),arm(.35,-12,0 if 'rush' in name else 42),arm(.72 if 'rush' in name else .58)],[(0,0),(hit,0 if 'rush' in name else -sign*65),(.72 if 'rush' in name else .58,0)])
    for name in ('recover','dual_recover'):
        nm=name+suffix
        make(nm,[zero(),zero(.25)],[arm(0),arm(.25)])
make('equip',[zero(),zero(.6)],[arm(0),arm(.6)])
# Set the cutting plane from the actual F5 shoulder stroke, not a guessed roll.
# Rotating around the blade axis keeps the hilt and tip fixed; this happens on
# preparation, with a stable edge through impact. No rotationTo/axis replacement.
import math
import numpy as np
from third import axis, dR, gp, part_m, tp_item_m, apply4, WORLD, VANILLA_ARM, D
hilt,tip=axis(p.model,p.model['display']['firstperson_righthand'],p.grip)
def edge_roll(name, at=None):
    clip=anims[name];source=anims[clip['third']['source']]
    sign=-1 if name.endswith('_off') else 1
    tp=p.model['display']['thirdperson_'+('left' if sign==-1 else 'right')+'hand']
    def matrix(t):
        r,_=sample(source,t);x=VANILLA_ARM[0]+D(r[0]);y=sign*D(r[1]);z=sign*D(r[2])
        body=np.clip(y*root['third']['body'] if not name.startswith('dual') else 0,-D(25),D(25))
        return tp_item_m(WORLD@part_m([-sign*5*math.cos(body),2,sign*5*math.sin(body)],x,y,z),tp,p.grip,None,sign)
    at=clip['impact'] if at is None else at;mid=matrix(at)
    velocity=apply4(matrix(at+.005),np.array([tip])/16)[0]-apply4(matrix(at-.005),np.array([tip])/16)[0]
    v=np.linalg.inv(mid[:3,:3])@velocity;v/=np.linalg.norm(v)
    direction=(tip-hilt);direction/=np.linalg.norm(direction)
    n=np.array([0.,0,1]);parallel=direction*(direction@n);perp=n-parallel
    aa=perp@v;bb=np.cross(direction,perp)@v;cc=parallel@v
    angle=math.acos(np.clip(-cc/math.hypot(aa,bb),-1,1));phase=math.atan2(bb,aa)
    candidates=[(math.degrees(phase+d)+180)%360-180 for d in (angle,-angle)]
    return round(min(candidates,key=abs),4)
for name in ('attack1','attack2','attack3','dual_attack1','dual_attack1_off','dual_attack3','dual_attack3_off'):
    angle=edge_roll(name)
    if name.startswith('dual_attack1'):
        times=[(0,0),(.29,angle),(.79,angle)]
        # The next thrust must begin from precisely this last wrist pose.
        next_name=name.replace('attack1','attack2')
        anims[anims[next_name]['third']['wrist_source']]['keyframes'][0]['rotation'][0]=angle
    elif name.startswith('dual_attack3'):times=[(0,0),(.42,angle),(.82,angle),(1.05,angle),(1.28,0)]
    elif name=='attack1':times=[(0,0),(.27,angle),(.63,angle),(.84,0)]
    elif name=='attack2':times=[(0,0),(.38,angle),(.53,angle),(.87,angle),(1.17,0)]
    else:times=[(0,0),(.23,angle),(.74,angle),(1.04,0)]
    wrist=anims[anims[name]['third']['wrist_source']]
    wrist['keyframes']=[dict(t=t,rotation=[roll,0,0],ease='ease_in_out') for t,roll in times]
root['_comment']='D-026: new poses from updated authored Display. F5 shoulders lead, wrist edge roll only; no artistic reflection.'
PATH.write_text(json.dumps(root,ensure_ascii=False,indent=2)+'\n')
