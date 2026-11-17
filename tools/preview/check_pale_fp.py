"""Screen-space FP contract, with vanilla equip explicitly represented (Minecraft 1.21.1).
The old preview checked only the item matrix below the vanilla hand transform.
"""
import hashlib
from pathlib import Path
from tl import *
a,model,fp,tp,grip=load('pale_linear_greatsword');h,tip=axis(model,fp,grip);mesh=model_points(model)/16
assert a['fp_pose_version']=='raised_inward_v1'
reference=json.load(open(Path(__file__).parent/'references/pale_f5_accepted_ee55b6e.json'))
# JSON 0 and 0.0 describe the same pose; editor serialization must not break the snapshot.
def numeric_canonical(v):
 if isinstance(v,dict):return {k:numeric_canonical(x)for k,x in v.items()}
 if isinstance(v,list):return [numeric_canonical(x)for x in v]
 if isinstance(v,(int,float))and not isinstance(v,bool):return float(v)
 return v
f5=dict(reference['numeric_canonical_f5'])
for name,c in a['animations'].items():
 if c.get('f5_reference'):f5[name]=reference['numeric_canonical_f5'][c['f5_reference']]
# Only the requested R flight preparation and its landing entry change F5.
raised_f5={'dual_leap','dual_leap_off','dual_slam','dual_slam_off'}
for name,wanted in f5.items():
 if name in raised_f5:continue
 c=a['animations'][name];fix=c.get('third',{});src=fix.get('source')
 value={'fix':fix,'pose':a['animations'][src]if src else c,'timing':{'end':round(end_time(c),6),'out':round(max(.05,c.get('return',.3))if c.get('hold',0)>0 else .15,6)}}
 assert hashlib.sha256(json.dumps(numeric_canonical(value),sort_keys=True).encode()).hexdigest()==wanted,(name,'accepted F5 changed')
print('PASS:',len(f5)-len(raised_f5),'F5 snapshots unchanged from',reference['revision'],'; 4 R flight/landing entries intentionally revised and checked separately')
# Camera mount is shared by idle and all clips, matching WeaponAnims.apply.
gesture_fp_item_m=fp_item_m
def fp_item_m(*args,**kwargs):
 kwargs.setdefault('view_offset',a.get('firstperson_offset'))
 return gesture_fp_item_m(*args,**kwargs)
# Vanilla applyItemArmTransform translates Y=-.52-.6*equippedProgress.
# Main/off heights are independent. The fixed pale renderer passes equip=0 for both,
# including UseAnim.NONE charge. Exercise unequal values, not just a perfect pair.
c=a['animations']['axe_dual_slam'];co=a['animations']['axe_dual_slam_off'];r,p=sample(c,.2);rl,pl=sample(co,.2)
right=model['display']['firstperson_righthand'];left=model['display']['firstperson_lefthand']
base=apply4(fp_item_m(right,r,p,grip,1),np.array([grip,tip])/16)
for equipped in(0,.25,.5,.75,1):
 dropped=apply4(fp_item_m(right,r,p,grip,1,equipped=equipped),np.array([grip,tip])/16)
 assert np.allclose(dropped[:,1]-base[:,1],-.6*equipped)
fixed_left=apply4(fp_item_m(left,rl,pl,grip,-1,equipped=0),np.array([grip,tip])/16)
assert abs(base*np.array([-1,1,1])-fixed_left).max()<1e-5
# Assess actual 16:9 projected coordinates, not just unprojected world equality.
def screen(points):return points[:,:2]/(-points[:,2,None])
assert abs(screen(base)*np.array([-1,1])-screen(fixed_left)).max()<1e-5
print('PASS: vanilla independent equip offsets reproduced (up to .6 blocks); corrected FP pair matches in 16:9 screen space')
FOV=math.tan(D(35));ASPECT=16/9
for name in('axe_attack1','axe_attack2','axe_attack3','axe_dual_chop','axe_dual_slam','axe_dual_slam_final'):
 clip=a['animations'][name];r,p=sample(clip,clip['impact']);mat=fp_item_m(right,r,p,grip,1);vertices=apply4(mat,mesh);depth=-vertices[:,2]
 visible=(depth>.05)&(abs(vertices[:,0])<depth*FOV*ASPECT)&(abs(vertices[:,1])<depth*FOV)
 assert visible.mean()>.97,(name,'mesh hidden during strike',visible.mean())
 point=apply4(mat,np.array([tip])/16);xy=screen(point)[0]
 pixel_y=270-xy[1]/FOV*270
 assert 100<pixel_y<455,(name,'impact must remain above the HUD guide at y=500',pixel_y)
# Check the server hit time too: donor impact metadata can differ by a fraction
# of a tick, so visibility must not rely on checking only the artistic marker.
combo=json.load(open(COMBO+'/pale_linear_greatsword.json'))
for group in ('steps','dual_steps'):
 for step in combo[group]:
  for field,sign in(('anim',1),('off_anim',-1)):
   if not step.get(field):continue
   clip=a['animations'][step[field]];r,p=sample(clip,step['hit']/20)
   display=right if sign==1 else left;mat=fp_item_m(display,r,p,grip,sign);points=apply4(mat,mesh);depth=-points[:,2]
   good=(depth>.05)&(abs(points[:,0])<depth*FOV*ASPECT)&(abs(points[:,1])<depth*FOV)
   assert good.mean()>.97,(step[field],'blade hidden at actual combat hit',good.mean())
   if group=='dual_steps':
    normal=mat[:3,:3]@np.array([0.,0,1]);normal/=np.linalg.norm(normal)
    assert abs(normal[1])<.06,(step[field],'blade face points at ground',normal)
# First slash winds above the crosshair, then crosses downward; thrust extends
# forward on a stable axis. Preserve original timing/ease rather than slowing it.
clip=a['animations']['axe_attack1'];pts=[]
for at in(.1,.22,.34):
 r,p=sample(clip,at);pts.append(apply4(fp_item_m(right,r,p,grip,1),np.array([tip])/16)[0])
assert screen(np.array(pts))[0,1]>.4 and pts[0][1]>pts[1][1]>pts[2][1]
clip=a['animations']['axe_attack3'];before=sample(clip,.12);hit=sample(clip,.22)
hand0=apply4(fp_item_m(right,*before,grip,1),np.array([grip])/16)[0];hand1=apply4(fp_item_m(right,*hit,grip,1),np.array([grip])/16)[0]
assert hand1[2]<hand0[2]-.45 and np.linalg.norm(XYZ(before[0])-XYZ(hit[0]))<1e-4
for sign,suffix in((1,''),(-1,'_off')):
 display=right if sign==1 else left;clip=a['animations']['axe_dual_chop'+suffix]
 for at in(.08,.18,.30):
  r,p=sample(clip,at);points=apply4(fp_item_m(display,r,p,grip,sign),np.array([grip,tip])/16)
  assert points[1,0]*sign<points[0,0]*sign,('paired tip points outward',sign,at)
 clip=a['animations']['dual_charge'+suffix];r,p=sample(clip,1.35);points=apply4(fp_item_m(display,r,p,grip,sign),np.array([tip])/16)
 charge_y=270-screen(points)[0,1]/FOV*270
 assert charge_y<430,('charge must remain above HUD with 70px clearance',sign,charge_y)
print('PASS: full blade visible on hits; high curved opening, straight thrust, inward paired tips, raised charge')

charge_reference=json.load(open(Path(__file__).parent/'references/pale_charge_before_lift.json'))
for name,old in charge_reference['animations'].items():
 new=a['animations'][name]
 for k in old['keyframes']:
  r,p=sample(old,k['t']);rn,pn=sample(new,k['t'])
  assert np.linalg.norm(XYZ(r)-XYZ(rn))<1e-8,(name,'charge angle changed')
  assert np.max(abs(pn-p-np.array([0,6 if k['t']>0 else 0,0])))<1e-6,(name,'charge lifted twice / moved sideways')
print('PASS: charge is exactly +6px Y at the held keys; original angles/X/Z unchanged')

# Optional compiled integration audit. CI runs this after Gradle: don't assume
# equip=0 in the mathematical renderer without checking the actual callback.
if '--compiled' in sys.argv:
 import os,re,shutil,subprocess
 root=Path(__file__).resolve().parents[2]
 compiled=root/'build/classes/java/client/com/tarkrakov/tearsofink/client/mixin/ItemInHandRendererMixin.class'
 javap=str(Path(os.environ['JAVA_HOME'])/'bin/javap')if os.environ.get('JAVA_HOME')else shutil.which('javap')
 assert compiled.exists() and javap,'Build the client classes with JDK25 first'
 code=subprocess.check_output([javap,'-p','-c',str(compiled)],text=True).split('private void tearsofink$ownMotion',1)[1]
 lines=code.splitlines();index=next(i for i,l in enumerate(lines)if 'InterfaceMethod'in l and 'tearsofink$renderArmWithItem'in l)
 call='\n'.join(lines[index-12:index+1])
 assert re.search(r'fconst_0\s*\n\s*\d+: aload\s+6\s*\n\s*\d+: fconst_0',call),'Compiled call still forwards vanilla swing/equip'
 # The early-out must inspect BOTH swing and equip, not let pale's swing=0
 # bypass the equip correction; the NONE-use path must also reach correction.
 head=code.split('Field com/tarkrakov/tearsofink/weapon/combat/Archetype.BASTION',1)[0]
 guard=head.split('istore        13',1)[1]
 assert 'iload         13'not in guard and 'fload         5'in guard and 'fload         7'in guard,'Unequipped main hand bypasses the override'
 using=code.split('isUsingItem:',1)[0].split('istore        13',1)[1]
 assert 'iload         13'in using and 'ifne'in using,'Pale NONE-charge must not bypass correction'
 print('PASS: compiled renderer checks both swing/equip and passes neutral 0/0, including pale NONE-use; legacy path preserved')

 # Verify the new shared mount in the built client too: it must precede the
 # no-playing-clip return, otherwise idle and abilities would visibly pop.
 animation_class=compiled.parents[1]/'combat/WeaponAnims.class'
 animation_code=subprocess.check_output([javap,'-p','-c',str(animation_class)],text=True)
 application=animation_code.split('public static boolean apply(',1)[1].split('private static',1)[0]
 assert application.index('Field FIRST_PERSON_OFFSET')<application.index('PoseStack.translate:')<application.index('Field ARMS'),'Camera mount missing for idle / applied after clip lookup'
 reloader_code=subprocess.check_output([javap,'-p','-c',str(animation_class.with_name('WeaponAnims$1.class'))],text=True)
 assert 'String firstperson_offset'in reloader_code,'Compiled loader ignores shared camera mount'
 assert 'String firstperson_idle_offset'in reloader_code and 'Method idleWeight:'in application,'Compiled idle composition/fade missing'
 print('PASS: compiled client loads the shared FP mount and applies it before idle/clip lookup')
