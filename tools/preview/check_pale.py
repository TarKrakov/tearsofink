"""Current author contract: literal axe LMB (3/5 steps), independent pale paired skills."""
import copy,json
from pathlib import Path
import numpy as np
from run import load,resolve_model
from third import *
from tl import hand_pose
ROOT=Path(__file__).resolve().parents[2];assets=ROOT/'src/main/resources/assets/tears-of-ink'
a,m,fp,tp,g=load('pale_linear_greatsword');anims=a['animations'];h,tip=axis(m,fp,g)
axe=json.loads((assets/'weapon_anims/axe.json').read_text())
combos=ROOT/'src/main/resources/data/tears-of-ink/weapon_combos'
c=json.loads((combos/'pale_linear_greatsword.json').read_text());donor=json.loads((combos/'axe.json').read_text())
assert a['lmb_source']=='axe'
assert len(c['steps'])==3 and len(c['dual_steps'])==5
assert m['display']==resolve_model('pale_linear_great')['display']==resolve_model('pale_linear_greatsword')['display']
for name in('attack1','attack2','attack3','dual_chop','dual_chop_third','dual_slam','dual_slam_final'):
 expected=copy.deepcopy(axe['animations'][name])
 if expected.get('third',{}).get('source'):expected['third']['source']='axe_'+expected['third']['source']
 assert anims['axe_'+name+'_raw']==expected,('donor animation changed',name)
 if name!='dual_chop_third':
  mounted=copy.deepcopy(expected)
  mounted.setdefault('third',{})['source']='axe_'+(axe['animations'][name].get('third',{}).get('source')or name)+'_raw'
  for key in mounted['keyframes']:key.setdefault('translation',[0,0,0])[2]-=16
  mounted['camera_mount_z']=-16
  assert anims['axe_'+name]==mounted,('non-mount change to copied gesture',name)
for group in('steps','dual_steps'):
 for actual,expected in zip(c[group],donor[group]):
  for field in('duration','hit','speed','late','second_hit'):
   assert actual.get(field)==expected.get(field),(group,field,actual)
  for field in('anim','off_anim'):
   wanted=('axe_'+expected[field])if expected.get(field)else expected.get(field)
   assert actual.get(field)==wanted,(group,field)
print('PASS: 3 solo / 5 paired steps; seven literal raw axe clips including F5 source; FP mounting only -16px Z; donor timing/late/speed; authored Display retained')
active={st[field]for group in('steps','dual_steps')for st in c[group]for field in('anim','off_anim')if st.get(field)}
active.update(n+s for n in('dual_charge','dual_rush','dual_parry','dual_repel','dual_leap','dual_slam')for s in('','_off'))
vertices=model_points(m)/16;count=0
for name in active:
 clip=anims[name];sign=-1 if name.endswith('_off')else 1
 for at in np.linspace(0,end_time(clip)-1e-7,60):
  r,p=sample(clip,at)
  for sign in((1,-1)if name.startswith('axe_dual')else(sign,)):
   display=m['display']['firstperson_'+('right'if sign==1 else'left')+'hand']
   mm=fp_item_m(display,r,p,g,sign);points=apply4(mm,vertices);assert np.isfinite(points).all(),name
   actual=apply4(mm,np.array([g])/16)[0]
   expected=np.array([sign*.56,-.52,-.72])+np.array([sign*p[0],p[1],p[2]])/16+gp(g,display,sign)
   assert np.linalg.norm(actual-expected)<1e-8,(name,'FP pivot')
   arm,W,tp,handle,_,_,_,_=hand_pose('pale_linear_greatsword',m,g,clip,at,sign,a['third'])
   baseline=tp_item_m(np.eye(4),tp,handle,None,sign);moved=tp_item_m(np.eye(4),tp,handle,W,sign)
   assert np.linalg.norm(apply4(baseline,np.array([handle])/16)-apply4(moved,np.array([handle])/16))<1e-8,(name,'F5 pivot')
   count+=1
# Charge: BOTH handles go backwards (+Z) and upwards (+Y) from their own rest.
for sign,suffix in((1,''),(-1,'_off')):
 display=m['display']['firstperson_'+('right'if sign==1 else'left')+'hand'];clip=anims['dual_charge'+suffix]
 rest=apply4(fp_item_m(display,np.zeros(3),np.zeros(3),g,sign),np.array([g,tip])/16)
 r,p=sample(clip,1.35);held=apply4(fp_item_m(display,r,p,g,sign),np.array([g,tip])/16)
 assert held[0,1]>rest[0,1]+.1 and held[0,2]>rest[0,2]+.05,('handle back/up',sign,rest,held)
 assert held[1,1]<held[0,1] and held[1,0]*sign<held[0,0]*sign,('tip down/in',sign)
 # Guard is deliberately crossed: a prohibition on crossing would be the wrong test.
 guard=anims['dual_parry'+suffix];r,p=sample(guard,.3);pts=apply4(fp_item_m(display,r,p,g,sign),np.array([g,tip])/16)
 assert pts[0,0]*sign>0 and pts[1,0]*sign<0,('crossed guard missing',sign)
 r,p=sample(anims['dual_leap'+suffix],.7);up=apply4(fp_item_m(display,r,p,g,sign),np.array([tip])/16)[0]
 r,p=sample(anims['dual_slam'+suffix],.2);down=apply4(fp_item_m(display,r,p,g,sign),np.array([tip])/16)[0]
 assert up[1]>0 and down[1]<-.5 and abs(up[0]-down[0])<.05,('straight vertical slam',sign,up,down)
 assert abs(anims['dual_slam'+suffix]['impact']-.2)<1e-8
print('PASS:',count,'finite full-mesh/pivot samples; handles back/up, tips down/in; crossed guard; straight raised-to-ground slam at server tick 4')
