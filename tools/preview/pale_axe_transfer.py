"""Author-requested literal axe LMB transfer, plus the pale sword's own paired skills.
Animations are deep copies, with namespaced source links; donor assets never change.
"""
import copy,json,math
from pathlib import Path
import numpy as np
from blade_pose import BladePose
from third import *
from hilt import euler_xyz
ROOT=Path(__file__).resolve().parents[2]
ANIM=ROOT/'src/main/resources/assets/tears-of-ink/weapon_anims'
COMBO=ROOT/'src/main/resources/data/tears-of-ink/weapon_combos'
p=BladePose('pale_linear_greatsword');a=p.anims;anims=a['animations']
axe=json.loads((ANIM/'axe.json').read_text());donor=json.loads((COMBO/'axe.json').read_text())
combo=json.loads((COMBO/'pale_linear_greatsword.json').read_text())
# Keep the sword's damage/reach/arc/knockback. Transfer the donor's sequence,
# timing, late windows and speed. Each axe step has one hit (including left-only).
old_solo=copy.deepcopy(combo['steps']);old_pair=copy.deepcopy(combo['dual_steps'])
# Re-running after the transfer must preserve these same sword values.
if a.get('lmb_source')=='axe':
 old_pair=[old_pair[0],old_pair[2],old_pair[4]]
for group,old,mapping in [('steps',old_solo,[0,1,2]),('dual_steps',old_pair,[0,0,1,1,2])]:
 output=[]
 for src,index in zip(donor[group],mapping):
  step=copy.deepcopy(src)
  for field in('damage','reach','arc','knockback'):step[field]=old[index][field]
  for field in('anim','off_anim'):
   if step.get(field):step[field]='axe_'+step[field]
  output.append(step)
 combo[group]=output
for name in('attack1','attack2','attack3','dual_chop','dual_chop_third','dual_slam','dual_slam_final'):
 clip=copy.deepcopy(axe['animations'][name])
 if clip.get('third',{}).get('source'):clip['third']['source']='axe_'+clip['third']['source']
 anims['axe_'+name+'_raw']=copy.deepcopy(clip)
 if name!='dual_chop_third':
  # Camera mounting only: the longer sword otherwise crosses the near plane.
  # Preserve unmodified donor F5 keys and all rotation/timing/easing channels.
  clip.setdefault('third',{})['source']='axe_'+(axe['animations'][name].get('third',{}).get('source') or name)+'_raw'
  for key in clip['keyframes']:key.setdefault('translation',[0,0,0])[2]-=16
  clip['camera_mount_z']=-16
 anims['axe_'+name]=clip
combo['early_ticks']=donor['early_ticks'];combo['_comment']='Автор: ЛКМ — серии топоров (3 одиночных / 5 парных), их timing/late/speed; урон/дальность/арка/отдача меча сохранены. R/ПКМ — прежняя механика меча.'
a['lmb_source']='axe';a['_comment']='ЛКМ дословно из axe (axe_*); парные ПКМ/R отдельно по новому описанию автора.'

def zero(t=0):return dict(t=t,rotation=[0,0,0],translation=[0,0,0],ease='ease_in_out')
def fp(t,d,h,left=False,edge=(1,0,0),ease='ease_in_out'):return p.key(t,d,edge,h,left,ease)
hilt,tip=axis(p.model,p.model['display']['firstperson_righthand'],p.grip)
# A separate source sets an anatomically placed arm and a complete blade frame.
# Invert the existing ThirdPerson.base target equation; no new renderer or IK.
def carried(t,delta,yaw,direction,edge,left=False,ease='ease_in_out'):
 sign=-1 if left else 1
 frame=p.model['display']['firstperson_'+('left' if left else 'right')+'hand']
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
 return dict(t=t,rotation=r.round(5).tolist(),translation=pos.round(5).tolist(),ease=ease)
def neutral_source(t,left=False):
 sign=-1 if left else 1;tp=p.model['display']['thirdperson_'+('left' if left else 'right')+'hand']
 item=MF@Rx(VANILLA_ARM[0])@C@dR(tp,sign)
 d=item@p.basis[:,0];e=item@p.basis[:,1];d[0]*=sign;e[0]*=sign
 return carried(t,0,0,d,e,left)
def make(name,keys,sources):
 c=anims[name];src=name+'_skill_third'
 c['keyframes']=keys;c['smooth']='tight';c['rotation_mode']='slerp';c['third']={'source':src}
 anims[src]={**{k:c[k] for k in('length','hold','return') if k in c},'smooth':'tight','rotation_mode':'slerp','keyframes':sources}
for left in(False,True):
 off='_off' if left else '';depth=.08 if left else 0
 # Relative to each authored rest, the hilt moves towards the camera and UP;
 # tips sink and converge. The depth offset keeps the intentionally crossed guard legible.
 charge_h=(.58,-.20,-.52) if left else(.50,-.15,-.34)
 charge_d=(-.53,-.54,-.65)
 charge=fp(1.35,charge_d,charge_h,left)
 make('dual_charge'+off,[zero(),fp(.5,(-.26,-.14,-.95),(.52,-.29,-.58) if not left else(.66,-.34,-.65),left),charge],
      [neutral_source(0,left),carried(.5,-25,12,(-.15,-.2,-.97),(1,0,0),left),carried(1.35,-45,15,(-.40,-.56,-.73),(1,0,0),left)])
 # The charge enters the same existing dash, now from its new held pose.
 thrust=fp(.12,(-.05,.08,-1),(.26,-.35,-1.15-depth),left,ease='linear')
 thrust_s=carried(.12,-18,5,(-.05,.08,-1),(1,0,0),left,'linear')
 make('dual_rush'+off,[dict(charge,t=0),thrust,dict(thrust,t=.35),zero(.72)],
      [carried(0,-45,15,(-.40,-.56,-.73),(1,0,0),left),thrust_s,dict(thrust_s,t=.35),neutral_source(.72,left)])
 def plane_edges(start,end):
  d=np.array(start,float);v=np.array(end,float);normal=np.cross(d,v);normal/=np.linalg.norm(normal)
  return np.cross(normal,d),np.cross(normal,v)
 g_edge,s_edge=plane_edges((-.65,.65,-.40),(.84,.12,-.53))
 gs_edge,ss_edge=plane_edges((-.52,.65,-.55),(.84,.12,-.53))
 guard=fp(.22,(-.65,.65,-.40),(.36,-.45,-.90-depth),left,edge=g_edge)
 guard_s=carried(.22,-25,-15,(-.52,.65,-.55),gs_edge,left)
 make('dual_parry'+off,[zero(),guard,dict(guard,t=.55)],
      [neutral_source(0,left),guard_s,dict(guard_s,t=.55)])
 spread=fp(.30,(.84,.12,-.53),(.50,-.38,-1.0-depth),left,edge=s_edge)
 make('dual_repel'+off,[dict(guard,t=0),spread],
      [dict(guard_s,t=0),carried(.30,-18,55,(.84,.12,-.53),ss_edge,left)])
 raised=fp(.34,(.04,.97,-.24),(.29,-.20,-.95-depth),left,edge=(0,-1,0))
 raised_s=carried(.34,-65,5,(.04,.99,-.13),(0,-1,0),left)
 make('dual_leap'+off,[zero(),raised,dict(raised,t=.7)],
      [neutral_source(0,left),raised_s,dict(raised_s,t=.7)])
 make('dual_slam'+off,[dict(raised,t=0),fp(.20,(.02,-.70,-.71),(.29,-.30,-1.12-depth),left,edge=(0,-1,0),ease='linear'),fp(.38,(.02,-.82,-.57),(.29,-.34,-1.16-depth),left,edge=(0,-1,0)),zero(.65)],
      [dict(raised_s,t=0),carried(.20,-40,5,(.02,-.93,-.37),(0,-1,0),left,'linear'),carried(.38,-25,5,(.02,-.96,-.28),(0,-1,0),left),neutral_source(.65,left)])
# Prepare the response's edge against the actual carried trajectory, using
# the same leading-edge operation as edge.py. Hilt, axis and timing stay fixed.
for name in ('dual_repel','dual_repel_off'):
 sign=-1 if name.endswith('_off') else 1
 side='left' if sign==-1 else 'right';display=p.model['display']['firstperson_'+side+'hand'];tp=p.model['display']['thirdperson_'+side+'hand']
 source_name=anims[name]['third']['source']
 for target_name,third_person in ((name,False),(source_name,True)):
  clip=anims[target_name];old=copy.deepcopy(clip)
  def matrix(at):
   if not third_person:
    r,pos=sample(old,at);return fp_item_m(display,r,pos,p.grip,sign)
   x,y,W,_=solve4(p.model['display'],sign,old,at,a['third'],{},p.grip,hilt,tip)
   return tp_item_m(WORLD@part_m([-sign*5,2,0],x,y,0),tp,p.grip,W,sign)
  keys=[copy.deepcopy(old['keyframes'][0])]
  for at in(.06,.09,.12,.15,.18,.24):
   before=matrix(at-.002);after=matrix(at+.002);mid=matrix(at)
   velocity=apply4(after,np.array([tip])/16)[0]-apply4(before,np.array([tip])/16)[0]
   direction=mid[:3,:3]@p.basis[:,0];direction/=np.linalg.norm(direction)
   edge=velocity-direction*(velocity@direction);edge/=np.linalg.norm(edge)
   if edge@(mid[:3,:3]@p.basis[:,1])<0:edge=-edge
   orientation=np.column_stack([direction,edge,np.cross(direction,edge)])@p.basis.T
   r=euler_xyz(orientation@dR(display,sign).T);r[1:]*=sign
   _,pos=sample(old,at)
   keys.append(dict(t=at,rotation=r.round(5).tolist(),translation=pos.round(5).tolist(),ease='linear'))
  keys.append(copy.deepcopy(old['keyframes'][-1]));clip['keyframes']=keys
for name in('dual_slam','dual_slam_off'):anims[name]['impact']=.20
(ANIM/'pale_linear_greatsword.json').write_text(json.dumps(a,ensure_ascii=False,indent=2)+'\n')
(COMBO/'pale_linear_greatsword.json').write_text(json.dumps(combo,ensure_ascii=False,indent=2)+'\n')
