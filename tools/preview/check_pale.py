"""D-026 baseline, grip, combat-timed handoffs and full-mesh checks.
No historical screen-space assumptions about the old left Display or old pivot.
"""
import json
from pathlib import Path
import numpy as np
from run import load, resolve_model
from third import *
ROOT=Path(__file__).resolve().parents[2]
a,model,fp,tp,grip=load('pale_linear_greatsword');anims=a['animations']
combo=json.loads((ROOT/'src/main/resources/data/tears-of-ink/weapon_combos/pale_linear_greatsword.json').read_text())
assert len(combo['steps'])==len(combo['dual_steps'])==3
# Registered item -> alias -> authored parent. No stale technical left overrides.
registered=resolve_model('pale_linear_greatsword')
parent=resolve_model('pale_linear_great')
assert model['display']==registered['display']==parent['display']
assert model['elements']==parent['elements']
# Handle is the stepped diagonal below the guard, not the cubes' editor origins.
assert np.allclose(grip,[7.75,2,7.75])
handle,tip=axis(model,fp,grip);vertices=model_points(model)/16
active=['equip','ability','ability_off','rush','rush_off','recover','recover_off']
for st in combo['steps']+combo['dual_steps']:
    for field in ('anim','off_anim'):
        if field in st:active.append(st[field])
    c=anims[st['anim']]
    hits=[st['hit']/20]+([st['second_hit']/20] if 'second_hit'in st else [])
    assert any(abs(c['impact']-h)<.001 for h in hits),(st,c['impact'])
active+=['dual_'+n+s for n in ('charge','parry','repel','leap','slam','rush','recover') for s in ('','_off')]
count=0
for name in active:
    c=anims[name];sign=-1 if name.endswith('_off') else 1
    side='left' if sign==-1 else 'right'
    fp=model['display']['firstperson_'+side+'hand'];tp=model['display']['thirdperson_'+side+'hand']
    fx=dict(c['third']);source=anims[fx['source']]
    assert fx.get('arm_space') and not fx.get('mirror') and not fx.get('push') and not fx.get('follow_blade')
    if fx.get('wrist_source'):fx['_wrist_clip']=anims[fx['wrist_source']]
    assert all(x['t']<y['t'] for x,y in zip(c['keyframes'],c['keyframes'][1:])),name
    for at in np.linspace(0,end_time(c)-1e-6,100):
        r,p=sample(c,at);mat=fp_item_m(fp,r,p,grip,sign)
        actual=apply4(mat,np.array([grip])/16)[0]
        expected=np.array([sign*.56,-.52,-.72])+np.array([sign*p[0],p[1],p[2]])/16+gp(grip,fp,sign)
        assert np.linalg.norm(actual-expected)<1e-8,(name,at,'FP grip drift')
        mesh=apply4(mat,vertices);assert np.isfinite(mesh).all()
        x,y,W,P=solve4(model['display'],sign,source,at,a['third'],fx,grip,handle,tip)
        base=tp_item_m(np.eye(4),tp,grip,None,sign)
        moved=tp_item_m(np.eye(4),tp,grip,W,sign)
        assert np.linalg.norm(apply4(base,np.array([grip])/16)-apply4(moved,np.array([grip])/16))<1e-8,(name,at,'F5 grip drift')
        assert abs(np.linalg.det(W)-1)<1e-8
        count+=1
    r,p=sample(c,end_time(c));assert np.linalg.norm(XYZ(r)-np.eye(3))<1e-7 and np.linalg.norm(p)<1e-7,(name,'return baseline')
# First actions must start exactly from the author's baseline in both views.
for name in ('attack1','dual_attack1','dual_attack1_off','dual_charge','dual_charge_off','dual_parry','dual_parry_off'):
    c=anims[name];r,p=sample(c,0);assert np.linalg.norm(r)+np.linalg.norm(p)<1e-8,name
    assert np.linalg.norm(sample(anims[c['third']['source']],0)[0])<1e-8,name
for steps in (combo['steps'],combo['dual_steps']):
    for old,new in zip(steps,steps[1:]):
        for field in ('anim','off_anim'):
            if field not in old:continue
            c,d=anims[old[field]],anims[new[field]];at=old['duration']/20
            for cc,dd in ((c,d),(anims[c['third']['source']],anims[d['third']['source']])):
                r,p=sample(cc,at);r1,p1=sample(dd,0)
                assert np.linalg.norm(XYZ(r)-XYZ(r1))+np.linalg.norm(p-p1)<1e-4,(old[field],new[field],'handoff')
            old_w=anims[c['third']['wrist_source']];new_w=anims[d['third']['wrist_source']] if d['third'].get('wrist_source') else None
            if new_w is not None:
                assert abs(sample(old_w,at)[0][0]-sample(new_w,0)[0][0])<.001,(old[field],new[field],'edge handoff')
# Equivalent Euler representations must not create complete spins.
for aa,bb in (([0,179,0],[0,-179,0]),([0,0,0],[0,360,0])):
    r=mix_rotation(aa,bb,.5);assert np.linalg.norm(XYZ(r)-XYZ(aa))<.04
print('PASS: authored inherited baseline; unchanged handle center; full meshes finite; anchored grips; combat markers; FP/F5 handoffs; shortest rotations;',count,'samples')
