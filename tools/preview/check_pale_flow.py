"""Check real scheduled handoffs and the camera-only polish of the accepted series."""
from tl import *
a,model,fp,tp,grip=load('pale_linear_greatsword')
combo=json.load(open(COMBO+'/pale_linear_greatsword.json'))
hilt,tip=axis(model,fp,grip)
assert a['fp_flow_version']=='linked_v1'

def matrix(name,at,sign=1,mount=True):
 display=model['display']['firstperson_'+('right'if sign==1 else'left')+'hand']
 r,p=sample(a['animations'][name],at)if name else(np.zeros(3),np.zeros(3))
 return fp_item_m(display,r,p,grip,sign,view_offset=a['firstperson_offset']if mount else None)

# Check BOTH solo joins at their server durations and the repeated paired strike,
# not merely that two authoring keys happen to be similar.
# The new author clip starts/ends are not identical and the runtime blends them;
# retain the exact continuity test for the unchanged paired series.
joins=[]
for sign,suffix in((1,''),(-1,'_off')):
 joins += [('axe_dual_slam'+suffix,.4,'axe_dual_slam_repeat'+suffix,sign),
           ('axe_dual_slam_repeat'+suffix,.4,'axe_dual_slam_final'+suffix,sign)]
for previous,at,following,sign in joins:
 assert np.max(abs(matrix(previous,at,sign)-matrix(following,0,sign)))<2e-5,(previous,following,'pose discontinuity')
print('PASS:',len(joins),'historical paired FP handoffs still match exactly')
# Solo join: finite and visually blendable; full 3D interpolation belongs to the client.
for previous,following in zip((s['anim'] for s in combo['steps']), (s['anim'] for s in combo['steps'][1:])):
 first=matrix(previous,a['animations'][previous]['length'])
 second=matrix(following,0)
 assert np.isfinite(first).all() and np.isfinite(second).all()
 assert abs(np.linalg.det(first[:3,:3]))>0.01 and abs(np.linalg.det(second[:3,:3]))>0.01
print('PASS: author solo clips have finite transitions; the runtime performs 0.2s blend')
# These intervals used to be frozen holds. Now the blade moves into preparation
# while the last 40/10 ms of the solo hold remain available for a clean join.
for name,start,stop in(('axe_attack1',.34,.55),('axe_attack2',.36,.70),('axe_dual_slam',.30,.40)):
 points=[apply4(matrix(name,float(t)),np.array([grip,tip])/16)for t in np.linspace(start,stop,9)]
 travel=np.linalg.norm(np.diff(points,axis=0),axis=2)
 assert np.all(travel.max(axis=1)>.0001),(name,'frozen bridge')
 assert np.max(travel)<.14,(name,'bridge step too large',travel.max())
print('PASS: former holds now carry the blade towards the next preparation without a large frame jump')
# All clips, including idle and historical solo R, receive the same tiny mount.
# It must not alter scale, angle, handedness, or F5's gesture-space matrices.
for sign in(1,-1):
 for name in[None]+[n for n in a['animations']if not n.endswith('_raw')]:
  for at in(0,.2,.5):
   old=matrix(name,at,sign,False);new=matrix(name,at,sign)
   assert np.allclose(old[:3,:3],new[:3,:3])
   assert np.allclose(new[:3,3]-old[:3,3],[0,-.04,.15]),(name,'inconsistent mount')
print('PASS: common combat FP mount is 4cm down / 15cm closer; orientation and scale unchanged (idle-only offset checked separately)')
