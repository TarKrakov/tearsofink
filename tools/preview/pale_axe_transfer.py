"""Reuse axe LMB rhythm with author-requested compactness, synchronization and blade roll.
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
  # Author correction: compact the donor gesture without changing its rhythm.
  def compact(keys):
   for key in keys:
    r=key.setdefault('rotation',[0,0,0]);r[0]=max(-60,min(42,r[0]*.8));r[1]*=.85;r[2]*=.85
    v=key.setdefault('translation',[0,0,0]);v[0]*=.6;v[1]=min(2.5,v[1]*(.45 if v[1]<0 else .5));v[2]*=.6
  compact(clip['keyframes'])
  source=copy.deepcopy(axe['animations'][axe['animations'][name].get('third',{}).get('source') or name])
  compact(source['keyframes'])
  if name=='dual_chop':clip['hold']=source['hold']=.6 # right still held when left has finished
  source_name='axe_'+name+'_compact_third';anims[source_name]=source
  clip.setdefault('third',{})['source']=source_name
  for key in clip['keyframes']:key['translation'][2]-=9
  clip['camera_mount_z']=-9
  if name.startswith('dual_'):clip['rotation_mode']='slerp'
 anims['axe_'+name]=clip
combo['early_ticks']=donor['early_ticks'];combo['_comment']='Автор: ЛКМ — серии топоров (3 одиночных / 5 парных), их timing/late/speed; урон/дальность/арка/отдача меча сохранены. R/ПКМ — прежняя механика меча.'
a['lmb_compact']=dict(rotation=[.8,.85,.85],pitch_limits=[-60,42],translation=[.6,.45,.6],upper_y=2.5,mount_z=-9);a['lmb_source']='axe';a['_comment']='ЛКМ axe: уменьшенная амплитуда и синхронные самостоятельные off-клипы; кромка 3/4 довёрнута вокруг клинка. Одиночный R из 8b7dd6d.'

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
 c['keyframes']=copy.deepcopy(keys);c['smooth']='tight';c['rotation_mode']='slerp';c['third']={'source':src}
 anims[src]={**{k:c[k] for k in('length','hold','return') if k in c},'smooth':'tight','rotation_mode':'slerp','keyframes':copy.deepcopy(sources)}
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
# Author: turn the blades themselves edge-first on paired steps 3/4. The
# axial rotation preserves grip and tip, so it cannot move the carrying arm.
main=anims['axe_dual_slam'];source=anims[main['third']['source']]
def roll_clip(clip,third_person):
 old=copy.deepcopy(clip);at=main['impact'];frame=p.model['display']['firstperson_righthand']
 def matrix(time):
  if not third_person:
   r,pos=sample(old,time);return fp_item_m(frame,r,pos,p.grip,1)
  x,y,W,_=solve4(p.model['display'],1,old,time,a['third'],main['third'],p.grip,hilt,tip)
  return tp_item_m(WORLD@part_m([-5,2,0],x,y,0),p.model['display']['thirdperson_righthand'],p.grip,W,1)
 middle=matrix(at);velocity=apply4(matrix(at+.003),np.array([tip])/16)[0]-apply4(matrix(at-.003),np.array([tip])/16)[0]
 v=np.linalg.inv(middle[:3,:3])@velocity;v/=np.linalg.norm(v)
 axis=np.array(tip-hilt,float);axis/=np.linalg.norm(axis);normal=np.array([0.,0,1]);parallel=axis*(axis@normal);perp=normal-parallel
 aa=perp@v;bb=np.cross(axis,perp)@v;cc=parallel@v
 phase=math.atan2(bb,aa);theta=math.acos(np.clip(-cc/math.hypot(aa,bb),-1,1))
 angles=[(phase+d+math.pi)%(2*math.pi)-math.pi for d in(theta,-theta)]
 angle=min(angles,key=abs)
 local_axis=dR(frame,1)@((tip-hilt)*dS(frame));local_axis/=np.linalg.norm(local_axis)
 x,y,z=local_axis;K=np.array([[0,-z,y],[z,0,-x],[-y,x,0]])
 axial=np.eye(3)+math.sin(angle)*K+(1-math.cos(angle))*(K@K)
 times=([k['t']for k in old['keyframes']] if not third_person else sorted(set([k['t']for k in old['keyframes']]+[round(float(t),6)for t in np.arange(0,old['keyframes'][-1]['t'],1/480)])))
 keys=[]
 for time in times:
  r,pos=sample(old,time);new=euler_xyz(XYZ(r)@axial)
  key=dict(t=time,rotation=new.round(5).tolist(),translation=pos.round(5).tolist(),ease='linear')
  if not third_person:key['ease']=old['keyframes'][len(keys)].get('ease','ease_in_out')
  keys.append(key)
 clip['keyframes']=keys;clip['rotation_mode']='slerp';clip['blade_edge_roll']=round(math.degrees(angle),5)
roll_clip(main,False);roll_clip(source,True)
# FP composition is authored in camera space, independently of the frozen F5
# sources. Keep every donor time/ease: the change is mounting and blade facing.
def first_person(name,poses):
 clip=anims[name];old=clip['keyframes'];assert len(old)==len(poses)
 clip['keyframes']=[p.key(k['t'],d,(0,-1,0),hand,False,k.get('ease','ease_in_out')) for k,(d,hand) in zip(old,poses)]
 clip['rotation_mode']='slerp';clip.pop('camera_mount_z',None);clip.pop('blade_edge_roll',None)
 clip['fp_pose']='raised_inward_v1'
chop_low=((- .42,-.46,-.78),(.37,-.005,-.69))
first_person('axe_dual_chop',[
 ((-.32,.72,-.62),(.40,.10,-.67)),
 ((-.42,-.38,-.82),(.37,.02,-.73)),chop_low])
slam_low=((- .12,-.56,-.82),(.34,.015,-.74))
first_person('axe_dual_slam',[chop_low,
 ((-.12,.78,-.61),(.34,.14,-.72)),
 ((-.12,-.45,-.88),(.34,.04,-.78)),slam_low])
first_person('axe_dual_slam_final',[slam_low,
 ((-.12,.78,-.61),(.34,.14,-.74)),
 ((-.12,.80,-.59),(.34,.155,-.71)),
 ((-.12,-.45,-.88),(.34,.04,-.78)),slam_low])
# Solo order remains the axe's diagonal / overhead chop / thrust.
first_person('axe_attack1',[
 ((.08,.78,-.62),(.38,.15,-.68)),
 ((-.40,-.60,-.69),(.22,.04,-.65)),
 ((-.74,-.60,-.30),(.10,-.02,-.90))])
# The diagonal follows through left/down, rather than turning flat to the cut.
for k in anims['axe_attack1']['keyframes']:
 rot,pos=sample(anims['axe_attack1'],k['t']);mat=fp_item_m(p.model['display']['firstperson_righthand'],rot,pos,p.grip,1)
 direction=mat[:3,:3]@p.basis[:,0];hand=apply4(mat,np.array([p.grip])/16)[0]
 corrected=p.key(k['t'],direction,(-.8,-.6,0),hand,False,k['ease']);k.update(corrected)
first_person('axe_attack2',[
 ((-.74,-.60,-.30),(.10,-.02,-.90)),
 ((.02,.78,-.63),(.14,.15,-.78)),
 ((.02,-.52,-.85),(.14,.045,-.74)),
 ((.02,-.64,-.77),(.14,.015,-.73))])
first_person('axe_attack3',[
 ((.02,-.64,-.77),(.14,.015,-.73)),
 ((-.28,.03,-.96),(.26,.06,-.48)),
 ((-.28,.03,-.96),(.22,.06,-.96)),
 ((-.28,.03,-.96),(.23,.06,-.83))])
# Charge: translate the existing pose straight UP, retaining every angle and
# all F5 poses. Its release begins at that same raised held position.
for suffix in ('','_off'):
 for k in anims['dual_charge'+suffix]['keyframes']:
  if k['t']>0:k['translation'][1]+=6
 anims['dual_rush'+suffix]['keyframes'][0]['translation'][1]+=6
a['fp_pose_version']='raised_inward_v1'
# The diagonal sword is not a Y-axis axe. Retarget its off-hand through its
# own Display, rather than feeding both hands identical relative Euler angles.
# Both paired blades now share a single phase/axis/edge trajectory.
SX=np.diag([-1.,1.,1.]);disp=p.model['display'];right_fp=disp['firstperson_righthand'];left_fp=disp['firstperson_lefthand'];right_tp=disp['thirdperson_righthand']
for base in ('dual_chop','dual_slam','dual_slam_final'):
 main_name='axe_'+base;main=anims[main_name];off=copy.deepcopy(main);source=anims[main['third']['source']]
 last=main['keyframes'][-1]['t'];times=sorted(set([0,last]+[round(float(t),6)for t in np.arange(0,last,1/480)]+[k['t']for k in main['keyframes']]))
 off_keys=[];off_source=[]
 for at in times:
  r,pos=sample(main,at);mm=fp_item_m(right_fp,r,pos,p.grip,1)
  direction=mm[:3,:3]@p.basis[:,0];edge=mm[:3,:3]@p.basis[:,1]
  hand=apply4(mm,np.array([p.grip])/16)[0]
  original_key=next((k for k in main['keyframes']if abs(k['t']-at)<1e-7),None)
  if original_key is not None or at==0:
   off_keys.append(p.key(at,direction,edge,hand,True,original_key.get('ease','ease_in_out')if original_key else 'ease_in_out'))
  x,y,W,_=solve4(disp,1,source,at,a['third'],main['third'],p.grip,hilt,tip)
  item=WORLD[:3,:3]@Ry(y)@Rx(x)@C@W@dR(right_tp,1)
  off_source.append(carried(at,math.degrees(x-VANILLA_ARM[0]),math.degrees(y),item@p.basis[:,0],item@p.basis[:,1],True,'linear'))
 off['keyframes']=off_keys;off['rotation_mode']='slerp';off['smooth']='tight';off['third']={'source':main_name+'_off_compact_third'}
 anims[main_name+'_off']=off
 anims[off['third']['source']]={**{k:source[k]for k in('length','hold','return')if k in source},'smooth':'tight','rotation_mode':'slerp','keyframes':off_source}
for st in combo['dual_steps']:
 if st.get('off_anim'):st['off_anim']=st['off_anim']+'_off'
# Restore only the single-right-hand R, including preparation and recovery.
reference=json.loads((Path(__file__).parent/'references/pale_solo_r_8b7dd6d.json').read_text())
for name,clip in reference['animations'].items():anims[name]=copy.deepcopy(clip)
a['solo_r_reference']=reference['revision']
(ANIM/'pale_linear_greatsword.json').write_text(json.dumps(a,ensure_ascii=False,indent=2)+'\n')
(COMBO/'pale_linear_greatsword.json').write_text(json.dumps(combo,ensure_ascii=False,indent=2)+'\n')
