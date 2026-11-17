"""New solo retreat, R flight preparation, and idle-only composition contracts."""
from tl import *
a,m,fp,tp,g=load('pale_linear_greatsword');h,tip=axis(m,fp,g);mesh=model_points(m)/16
fov=math.tan(D(35))
for sign,suffix in((1,''),(-1,'_off')):
 c=a['animations']['dual_leap'+suffix];display=m['display']['firstperson_'+('right'if sign>0 else'left')+'hand']
 hands=[];heights=[]
 for at in(.34,.45,.55,.7):
  r,p=sample(c,at);mm=fp_item_m(display,r,p,g,sign,view_offset=fp_offset(m))
  points=apply4(mm,mesh);depth=-points[:,2]
  visible=(depth>.05)&(abs(points[:,0])<depth*fov*16/9)&(abs(points[:,1])<depth*fov)
  assert visible.mean()>.97,('flight blade out of frame',sign,at,visible.mean())
  hands.append(apply4(mm,np.array([g])/16)[0,1])
  pose=hand_pose('pale_linear_greatsword',m,g,c,at,sign,a['third'],True,a['animations'])
  arm,W,display_tp,handle=pose[:4]
  cm=tp_item_m(WORLD@part_m([-5*sign,2,0],*arm),display_tp,handle,W,sign)
  heights.append(apply4(cm,np.array([g])/16)[0,1])
 assert np.all(np.diff(hands)>0)and hands[-1]-hands[0]>.075,('hands do not rise in FP',hands)
 assert heights[-1]>heights[0]+.02,('carrying arm does not rise in F5',heights)
 slam=a['animations']['dual_slam'+suffix]
 r,p=sample(c,.7);rs,ps=sample(slam,0)
 assert np.allclose(XYZ(r),XYZ(rs))and np.allclose(p,ps),('landing entry does not match flight',sign)
print('PASS: both hands progressively rise during R flight in FP/F5; blades stay visible; landing begins from raised pose')
back=a['animations']['backstep'];balance=json.load(open(COMBO+'/../weapon_moves/pale_linear_greatsword.json'))['backstep']
assert abs(back['impact']-balance['hit_ticks']/20)<1e-6
r,p=sample(back,back['impact']);mm=fp_item_m(fp,r,p,g,1,view_offset=fp_offset(m));v=apply4(mm,mesh);depth=-v[:,2]
assert ((depth>.05)&(abs(v[:,0])<depth*fov*16/9)&(abs(v[:,1])<depth*fov)).mean()>.97
print('PASS: retreat slash is visible at configured server hit tick')
# Idle is 4cm farther than the accepted combat mount, for BOTH hands.
assert np.allclose(a['firstperson_idle_offset'],[0,0,-.64])
for sign in(1,-1):
 display=m['display']['firstperson_'+('right'if sign>0 else'left')+'hand']
 combat=fp_item_m(display,np.zeros(3),np.zeros(3),g,sign,view_offset=fp_offset(m))
 idle=fp_item_m(display,np.zeros(3),np.zeros(3),g,sign,view_offset=fp_offset(m,1))
 assert np.allclose(idle[:3,:3],combat[:3,:3])and np.allclose(idle[:3,3]-combat[:3,3],[0,0,-.04])
 sampled={}
 def observe(at,first,carried):
  if sign in first:sampled[round(at*120)]=first[sign]
 build('pale_linear_greatsword',[('axe_attack1','axe_dual_chop_off',.12),('backstep','axe_dual_chop_off',.6)],True,'unused.gif',fps=120,observer=observe,render_frames=False)
 assert np.allclose(sampled[0],idle),('idle-to-action pop',sign)
 assert np.allclose(sampled[max(sampled)],idle),('rest did not regain idle composition',sign)
print('PASS: idle-only 4cm forward offset preserves orientation and smoothly enters/returns through an interrupted action')
