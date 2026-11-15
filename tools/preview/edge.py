import sys, json, math
sys.path.insert(0,'.')
from twtest import *
from hilt import euler_xyz

def rodrigues(ax, ang):
    ax = ax/np.linalg.norm(ax)
    K = np.array([[0,-ax[2],ax[1]],[ax[2],0,-ax[0]],[-ax[1],ax[0],0]])
    return np.eye(3)+math.sin(ang)*K+(1-math.cos(ang))*K@K

def edge_align(clip, keys_idx, dt=0.02):
    disp = model['display']; fp = disp['firstperson_righthand']
    handle, tip = axis(model, fp, grip)
    pts = model_points(model)
    # выпуклость: середина клинка относительно хорды рукоять→кончик (в пикселях модели)
    chord = tip - handle; cu = chord/np.linalg.norm(chord)
    rel = pts - handle; perp = rel - np.outer(rel@cu, cu)
    bulge = perp.mean(0); bulge[2] = 0; bulge /= np.linalg.norm(bulge)
    Rd = dR(fp, 1)
    def tippos(t):
        rot, pos = sample(clip, t)
        Rr = XYZ(rot)
        h, tp_ = hand_tip(fp, 1, Rr, pos, grip, handle, tip)
        return tp_, h, Rr
    out = []
    for i in keys_idx:
        k = clip['keyframes'][i]; t = k['t']
        p1,_,_ = tippos(max(0,t-dt)); p2,_,_ = tippos(t+dt)
        v = p2 - p1
        Rr = XYZ(k['rotation'])
        R = Rr @ Rd
        a = R @ (chord/np.linalg.norm(chord))  # ось клинка в камере (направление)
        b = R @ bulge                          # выпуклость (кромка)
        # повернуть вокруг a так, чтобы b смотрела по движению (составляющая v поперёк оси)
        vp = v - (v@a)*a
        if np.linalg.norm(vp) < 1e-6: out.append(k['rotation']); continue
        vp /= np.linalg.norm(vp)
        bp = b - (b@a)*a; bp /= np.linalg.norm(bp)
        ang = math.atan2(np.cross(bp, vp)@a, bp@vp)
        Rn = rodrigues(a, ang) @ Rr
        e = [round(float(x),1) for x in euler_xyz(Rn)]
        print(t, k['rotation'], '->', e, 'turn', round(math.degrees(ang)))
        k['rotation'] = e
    return clip
