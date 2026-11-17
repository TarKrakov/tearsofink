"""Toggle guard geometry and actual refreshed timeline, independent from parrying."""
from tl import *
a,m,fp,tp,g=load('pale_linear_greatsword');anims=a['animations'];h,tip=axis(m,fp,g)
mesh=model_points(m)/16;fov=math.tan(D(35));basis=np.array([0.,0,1.])
balance=json.load(open(COMBO+'/../weapon_moves/pale_linear_greatsword.json'))['shift_guard']
assert 'single_duration_ticks'not in balance and 'dual_duration_ticks'not in balance
assert 0<balance['single_reduction']<balance['dual_reduction']<1
for paired in(False,True):
 for sign,suffix in((1,''),(-1,'_off')):
  name=('dual_guard'if paired else'guard')+suffix
  held=('dual_guard_hold'if paired else'guard_hold')+suffix
  c=anims[name];d=m['display']['firstperson_'+('right'if sign==1 else'left')+'hand']
  assert end_time(c)>3 and end_time(anims[held])>3 # longer than the 1.5s refresh period
  r,p=sample(c,.5);mm=fp_item_m(d,r,p,g,sign,view_offset=fp_offset(m))
  pts=apply4(mm,np.array([g,tip])/16);v=apply4(mm,mesh);depth=-v[:,2]
  assert ((depth>.05)&(abs(v[:,0])<depth*fov*16/9)&(abs(v[:,1])<depth*fov)).mean()>.97,(name,'out of frame')
  assert pts[0,0]*sign>0 and pts[1,0]*sign<0 and pts[1,1]>pts[0,1]+.3,(name,'diagonal does not cross the view')
  normal=mm[:3,:3]@basis;normal/=np.linalg.norm(normal)
  if paired:
   parry=anims['dual_parry'+suffix];rp,_=sample(parry,.4)
   assert np.linalg.norm(XYZ(r)-XYZ(rp))>.5,(name,'guard must differ from R parry')
   assert abs(normal[2])>.99,(name,'paired guard must present the FLATS')
  else:
   # Model's own blade-edge basis, not a guessed global roll axis.
   from blade_pose import BladePose
   b=BladePose('pale_linear_greatsword');edge=mm[:3,:3]@b.basis[:,1];edge/=np.linalg.norm(edge)
   assert edge[2]<-.99,(name,'solo edge must face outward to enemy')
  # Refresh starts from the exact held pose in both render paths.
  rh,ph=sample(anims[held],0);assert np.allclose(XYZ(r),XYZ(rh))and np.allclose(p,ph)
  x=hand_pose('pale_linear_greatsword',m,g,c,.5,sign,a['third'],True,anims)
  y=hand_pose('pale_linear_greatsword',m,g,anims[held],0,sign,a['third'],True,anims)
  assert np.allclose(x[0],y[0])and np.allclose(x[1],y[1]),(name,'F5 refresh pop')
 print('PASS:', 'paired broadside crossed blades'if paired else'solo diagonal, outward cutting edge', '; full geometry visible')
 base='dual_guard'if paired else'guard';hold='dual_guard_hold'if paired else'guard_hold'
 steps=[(base,base+'_off'if paired else None,1.5),(hold,hold+'_off'if paired else None,1.5),(hold,hold+'_off'if paired else None,.6),('guard_release','guard_release_off'if paired else None,.25)]
 snapshots={}
 def observe(t,first,carried):snapshots[round(t*120)]=(first,carried)
 build('pale_linear_greatsword',steps,paired,'unused.gif',fps=120,observer=observe,render_frames=False)
 for sign in((1,-1)if paired else(1,)):
  expected=snapshots[120][0][sign]
  for tick in range(120,432):assert np.allclose(snapshots[tick][0][sign],expected,atol=2e-5),('refresh destabilizes FP',paired,sign,tick)
  idle=fp_item_m(m['display']['firstperson_'+('right'if sign==1 else'left')+'hand'],np.zeros(3),np.zeros(3),g,sign,view_offset=fp_offset(m,1))
  assert np.allclose(snapshots[max(snapshots)][0][sign],idle)
 print('PASS: 120Hz held timeline across two refreshes; toggle-off returns to idle')
