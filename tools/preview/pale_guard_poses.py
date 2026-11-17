"""Author's diagonal outward-edge solo guard and broadside crossed paired guard.
Only guard clips are written; the accepted combat series and author model are retained.
"""
import copy,json
from pathlib import Path
import numpy as np
from blade_pose import BladePose
from third import *
from hilt import euler_xyz
p=BladePose('pale_linear_greatsword');a=p.anims;anims=a['animations'];hilt,tip=axis(p.model,p.model['display']['firstperson_righthand'],p.grip)
# Same inverse ThirdPerson.base calibration as the accepted skill generator.
def carried(t,delta,yaw,direction,edge,left=False):
 sign=-1 if left else 1;frame=p.model['display']['firstperson_'+('left'if left else'right')+'hand']
 d=np.array(direction,float);d[0]*=sign;d/=np.linalg.norm(d)
 e=np.array(edge,float);e[0]*=sign;e-=d*(d@e);e/=np.linalg.norm(e)
 item=np.column_stack([d,e,np.cross(d,e)])@p.basis.T
 relative=item@dR(frame,sign).T
 x=VANILLA_ARM[0]+D(delta);y=sign*D(yaw)
 camera_direction=MF@(Ry(y)@Rx(x)@np.array([0.,1.,0.]))
 q=np.array([sign*5/16,-.245,0])+camera_direction*np.array([1/a['third']['reach'],1/a['third']['reach'],1])*.85
 hand=q-a['third']['lead']*(item@((tip-hilt)/16*dS(frame)))
 rest=np.array([sign*.56,-.52,-.72])+gp(p.grip,frame,sign)
 pos=(hand-rest)*16;pos[0]*=sign;r=euler_xyz(relative);r[1:]*=sign
 return dict(t=t,rotation=r.round(5).tolist(),translation=pos.round(5).tolist(),ease='ease_in_out')
def zero(t=0):return dict(t=t,rotation=[0,0,0],translation=[0,0,0],ease='ease_in_out')
for pair in(False,True):
 for left in(False,True):
  name=('dual_guard'if pair else'guard')+('_off'if left else'')
  hold=('dual_guard_hold'if pair else'guard_hold')+('_off'if left else'')
  d=(-.76,.65,0)if pair else(-.84,.54,-.04)
  # Paired edges point out/down to the sides; blade flats face camera/enemy.
  # Solo edge points towards the enemy (-Z), not towards the camera.
  e=(-.65,-.76,0)if pair else(0,0,-1)
  hand=(.30,-.16,-.72 if pair and left else-.70)
  key=p.key(.26,d,e,hand,left,'ease_in_out')
  src=carried(.26,-62,-32 if pair else-12,d,e,left)
  base=dict(length=3.5,hold=3,return_=.25,impact=-1,smooth='tight',rotation_mode='slerp')
  base['return']=base.pop('return_')
  anims[name]={**base,'keyframes':[zero(),key],'third':{'source':name+'_shoulder'}}
  anims[name+'_shoulder']={**base,'keyframes':[zero(),src]}
  anims[hold]={**base,'keyframes':[dict(copy.deepcopy(key),t=0)],'third':{'source':hold+'_shoulder'}}
  anims[hold+'_shoulder']={**base,'keyframes':[dict(copy.deepcopy(src),t=0)]}
for suffix in('','_off'):
 anims['guard_release'+suffix]=dict(length=.25,hold=0,impact=-1,smooth='tight',rotation_mode='slerp',keyframes=[zero()])
a['guard_pose_version']='diagonal_and_broadside_cross_v1'
path=Path(__file__).resolve().parents[2]/'src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword.json'
path.write_text(json.dumps(a,ensure_ascii=False,indent=2)+'\n')
