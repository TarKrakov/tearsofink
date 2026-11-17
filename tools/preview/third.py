# Превью третьего лица: та же математика, что в ThirdPerson.java (держать в согласии!).
import json, math, sys, os
import numpy as np
from PIL import Image, ImageDraw

import os
REPO = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../src/main/resources/assets/tears-of-ink')
D = math.radians

def Rx(a):
    c, s = math.cos(a), math.sin(a); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def Rz(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c,-s,0],[s,c,0],[0,0,1]])
def XYZ(v):  # JOML rotationXYZ (градусы)
    return Rx(D(v[0])) @ Ry(D(v[1])) @ Rz(D(v[2]))

def M4(R=None, t=None, s=None):
    m = np.eye(4)
    if R is not None: m[:3,:3] = R
    if s is not None: m[:3,:3] = m[:3,:3] @ np.diag(s)
    if t is not None: m[:3,3] = t
    return m
def T(v): return M4(t=np.array(v, float))

# ---------- движения (порт WeaponAnims.sample) ----------
def ease(e, u):
    e = e.upper()
    if e == 'LINEAR': return u
    if e == 'EASE_IN': return u**3
    if e == 'EASE_OUT': return 1-(1-u)**3
    if e == 'EASE_IN_OUT': return 4*u**3 if u < .5 else 1-(-2*u+2)**3/2
    if e == 'BACK_OUT':
        c1 = 1.70158; c3 = c1+1; return 1+c3*(u-1)**3+c1*(u-1)**2
    if e == 'HEAVY': return u**4
    return u

def key(k):
    return dict(t=k['t'], rot=np.array(k.get('rotation',[0,0,0]),float), pos=np.array(k.get('translation',[0,0,0]),float),
                ease=k.get('ease','ease_in_out'))
REST = dict(t=0, rot=np.zeros(3), pos=np.zeros(3), ease='ease_in_out')

def spline(p0,p1,p2,p3,u):
    return 0.5*(2*p1+(-p0+p2)*u+(2*p0-5*p1+4*p2-p3)*u*u+(-p0+3*p1-3*p2+p3)*u**3)

def _slope(b, a):
    m = (b+a)/2; lim = 3*np.minimum(abs(b), abs(a))
    return np.where(b*a <= 0, 0, np.sign(m)*np.minimum(abs(m), lim))

def tight(p0,p1,p2,p3,ue,uh):  # "smooth": "tight" — не перелетает позы (как WeaponAnims.tight)
    u=uh; h00=2*u**3-3*u**2+1; h10=u**3-2*u**2+u; h01=-2*u**3+3*u**2; h11=u**3-u**2
    s1=_slope(p1-p0,p2-p1); s2=_slope(p2-p1,p3-p2)
    return np.where((s1==0)&(s2==0), p1+(p2-p1)*ue, h00*p1+h10*s1+h01*p2+h11*s2)

def sample(c, t):
    keys = [key(k) for k in c['keyframes']]; keys.sort(key=lambda k: k['t'])
    hold = c.get('hold',0); back = c.get('return',0.3); L = c['length']
    if hold > 0 and keys and t >= keys[-1]['t']:
        last = keys[-1]; since = t-last['t']-hold
        if since <= 0: return last['rot'], last['pos']
        u = ease('ease_in_out', min(1, since/max(.01,back)))
        return last['rot']*(1-u), last['pos']*(1-u)
    pts = []
    if not keys or keys[0]['t'] > 0: pts.append(REST)
    pts += keys
    if hold <= 0 or not keys: pts.append(dict(REST, t=L))
    i = 0
    while i < len(pts)-2 and pts[i+1]['t'] <= t: i += 1
    a = pts[i]; b = pts[min(i+1,len(pts)-1)]
    u = min(1,max(0,(t-a['t'])/(b['t']-a['t']))) if b['t'] > a['t'] else 1
    raw = u; u = ease(a['ease'], u)
    if not c.get('smooth'):
        return a['rot']+(b['rot']-a['rot'])*u, a['pos']+(b['pos']-a['pos'])*u
    a0 = pts[max(i-1,0)]; b1 = pts[min(i+2,len(pts)-1)]
    if c.get('smooth') == 'tight':
        uh = raw if a['ease'] == 'ease_in_out' else u
        return tight(a0['rot'],a['rot'],b['rot'],b1['rot'],u,uh), tight(a0['pos'],a['pos'],b['pos'],b1['pos'],u,uh)
    return spline(a0['rot'],a['rot'],b['rot'],b1['rot'],u), spline(a0['pos'],a['pos'],b['pos'],b1['pos'],u)

_sample_euler = sample

def mix_rotation(a, b, u):
    from gif import slerp_m
    r = XYZ(a) @ slerp_m(XYZ(a).T @ XYZ(b), u)
    return np.degrees([math.atan2(-r[1,2],r[2,2]), math.asin(np.clip(r[0,2],-1,1)), math.atan2(-r[0,1],r[0,0])])

def sample(c, t):
    rot, pos = _sample_euler(c, t)
    if c.get('rotation_mode') != 'slerp' or not c['keyframes']: return rot, pos
    keys = sorted((key(k) for k in c['keyframes']), key=lambda k:k['t'])
    last = keys[-1]
    if c.get('hold',0) > 0 and t >= last['t']:
        u = ease('ease_in_out', np.clip((t-last['t']-c['hold'])/max(.01,c.get('return',.3)),0,1))
        return mix_rotation(last['rot'],np.zeros(3),u),pos
    a=REST; b=dict(REST,t=c['length'])
    for k in keys:
        if k['t'] > t: b=k;break
        a=k
    u=np.clip((t-a['t'])/(b['t']-a['t']),0,1) if b['t']>a['t'] else 1
    return mix_rotation(a['rot'],b['rot'],ease(a['ease'],u)),pos

def end_time(c):
    keys = c['keyframes']
    if c.get('hold',0) > 0 and keys: return max(k['t'] for k in keys)+c['hold']+c.get('return',0.3)
    return c['length']

# ---------- третье лицо (порт ThirdPerson.solve) ----------
HAND = np.array([0.56, -0.52, -0.72])      # ванильная точка руки от первого лица
SHOULDER = np.array([5/16, -0.245, 0])     # правое плечо относительно глаз
MF = np.diag([-1.,-1.,1.])                 # камера → пространство модели игрока
C = Rx(D(-90)) @ Ry(D(180))                # ItemInHandLayer
LAYER_T = np.array([1/16, 0.125, -0.625])

def grip_point(grip, disp):
    s = np.array(disp.get('scale',[1,1,1]),float)
    v = (np.array(grip)/16-0.5)*s
    v = XYZ(disp.get('rotation',[0,0,0])) @ v
    return v + np.array(disp.get('translation',[0,0,0]))/16

def solve(rot, pos, fp, tp, grip, tune):
    Ofp = XYZ(rot) @ XYZ(fp.get('rotation',[0,0,0]))
    P = HAND + pos/16 + (grip_point(grip, fp) if grip is not None else 0)
    v = P - SHOULDER
    v = v * np.array([tune['reach'], tune['reach'], 1])
    d = MF @ (v/np.linalg.norm(v))
    x = -math.acos(max(-1,min(1,d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-3 else 0
    A = Ry(y) @ Rx(x)
    W = np.linalg.inv(A @ C) @ (MF @ Ofp) @ np.linalg.inv(XYZ(tp.get('rotation',[0,0,0])))
    body = max(-D(tune['body_max']), min(D(tune['body_max']), y*tune['body']))
    return x, y, body, W

# ---------- геометрия ----------
def cubes_of(model):
    out = []
    for e in model['elements']:
        f, t = np.array(e['from'])/16, np.array(e['to'])/16
        R = np.eye(3); o = np.zeros(3)
        if 'rotation' in e and e['rotation'].get('angle',0):
            ax = e['rotation']['axis']; a = D(e['rotation']['angle'])
            R = {'x':Rx,'y':Ry,'z':Rz}[ax](a); o = np.array(e['rotation']['origin'])/16
        out.append((f, t, R, o))
    return out

def box_faces(f, t):
    x0,y0,z0 = f; x1,y1,z1 = t
    v = np.array([[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]])
    idx = [[0,1,2,3],[4,5,6,7],[0,1,5,4],[2,3,7,6],[0,3,7,4],[1,2,6,5]]
    return v, idx

def apply4(m, pts):
    p = np.c_[pts, np.ones(len(pts))] @ m.T
    return p[:,:3]

def item_quads(model, m, color):
    q = []
    for f, t, R, o in cubes_of(model):
        v, idx = box_faces(f, t)
        v = (v-o) @ R.T + o
        v = apply4(m, v)
        for i in idx: q.append((v[i], color))
    return q

def part_quads(m, origin, size, color):
    v, idx = box_faces(np.array(origin)/16, (np.array(origin)+np.array(size))/16)
    v = apply4(m, v)
    return [(v[i], color) for i in idx]

def part_m(pivot, rx=0, ry=0, rz=0):
    return T(np.array(pivot)/16) @ M4(R=Rz(rz)@Ry(ry)@Rx(rx))

def disp_m(disp, left=False):
    t = np.array(disp.get('translation',[0,0,0]),float)/16
    r = np.array(disp.get('rotation',[0,0,0]),float)
    if left: t[0] = -t[0]; r[1] = -r[1]; r[2] = -r[2]
    return T(t) @ M4(R=XYZ(r)) @ M4(s=np.array(disp.get('scale',[1,1,1]),float)) @ T([-.5,-.5,-.5])

# ---------- рендер ----------
def render(quads, eye, target, size=(260,260), fov=60, bg=(32,34,48)):
    img = Image.new('RGB', size, bg); dr = ImageDraw.Draw(img)
    eye = np.array(eye,float); target = np.array(target,float)
    fwd = target-eye; fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, [0,1,0]); right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = size[1]/2/math.tan(D(fov)/2)
    light = np.array([0.4,0.8,0.5]); light /= np.linalg.norm(light)
    items = []
    for v, col in quads:
        rel = v-eye
        z = rel @ fwd
        if np.any(z <= 0.05): continue
        n = np.cross(v[1]-v[0], v[2]-v[0]); nn = np.linalg.norm(n)
        sh = 0.55+0.45*abs(n@light/nn) if nn > 1e-9 else 1
        pts = [(size[0]/2+f*(r@right)/zz, size[1]/2-f*(r@up)/zz) for r, zz in zip(rel, z)]
        items.append((z.mean(), pts, tuple(int(c*sh) for c in col)))
    items.sort(key=lambda a: -a[0])
    for _, pts, col in items: dr.polygon(pts, fill=col, outline=tuple(int(c*0.6) for c in col))
    return img

WORLD = M4(R=np.diag([-1.,-1.,1.])) @ T([0,-1.501,0])   # модель → мир (как LivingEntityRenderer), лицом к -z

def steve_and_items(arms, wrist, items, body_yaw):
    """arms: {'R':(x,y,z) или None}, items: {'R':(model,fp,tp,grip)}"""
    q = []
    base = WORLD
    q += part_quads(base @ part_m([0,0,0]), [-4,-8,-4], [8,8,8], (200,170,140))
    q += part_quads(base @ part_m([0,0,0], 0, body_yaw), [-4,0,-2], [8,12,4], (60,90,150))
    for s, piv in (('R',[-5,2,0]),('L',[5,2,0])):
        sgn = 1 if s == 'R' else -1
        piv = [-5*math.cos(body_yaw)*sgn, 2, 5*math.sin(body_yaw)*sgn]
        x, y, z = arms[s]
        am = base @ part_m(piv, x, y, z)
        q += part_quads(am, [-3 if s=='R' else -1,-2,-2], [4,12,4], (200,170,140) if s=='R' else (180,150,120))
        if s in items:
            model, fp, tp, grip = items[s]
            left = s == 'L'
            m = am @ M4(R=C) @ T([(-1 if left else 1)/16, .125, -.625])
            W = wrist.get(s)
            if W is not None:
                g = np.zeros(3)
                if grip is not None:
                    gp = grip_point(grip, tp)
                    g = gp if not left else gp*np.array([-1,1,1])
                m = m @ T(g) @ M4(R=W) @ T(-g)
            m = m @ disp_m(tp, left)
            q += item_quads(model, m, (190,40,50))
    for piv in ([-1.9,12,0],[1.9,12,0]):
        q += part_quads(base @ part_m(piv), [-2,0,-2], [4,12,4], (50,60,90))
    return q

def fp_quads(model, fp, rot, pos, grip, left=False):
    m_ = -1 if left else 1
    m = T([m_*0.56, -0.52, -0.72]) @ T([m_*pos[0]/16, pos[1]/16, pos[2]/16])
    R = Rx(D(rot[0])) @ Ry(D(m_*rot[1])) @ Rz(D(m_*rot[2]))
    if grip is not None:
        g = grip_point(grip, fp) * np.array([m_,1,1])
        if left:  # gripPoint с зеркалом
            s = np.array(fp.get('scale',[1,1,1]),float); r = fp.get('rotation',[0,0,0])
            v = (np.array(grip)/16-.5)*s
            v = (Rx(D(r[0]))@Ry(D(-r[1]))@Rz(D(-r[2]))) @ v
            t_ = np.array(fp.get('translation',[0,0,0]))/16
            g = v + np.array([-t_[0],t_[1],t_[2]])
        m = m @ T(g) @ M4(R=R) @ T(-g)
    else:
        m = m @ M4(R=R)
    m = m @ disp_m(fp, left)
    return item_quads(model, m, (190,40,50))

VANILLA_ARM = (-math.pi/10, 0, 0)

# ---------- ось оружия: рукоять и кончик (как в ThirdPerson.axis) ----------
def model_points(model):
    pts = []
    for f, t, R, o in cubes_of(model):
        v, _ = box_faces(f, t)
        pts.append((v-o) @ R.T + o)
    return np.vstack(pts)*16  # в пикселях

def axis(model, fp, grip):
    """Рукоять и кончик в пикселях модели. Без grip: концы главной оси, рукоять — тот, что ближе к руке в стойке."""
    p = model_points(model)
    if grip is not None:
        g = np.array(grip, float)
        tip = p[np.argmax(((p-g)**2).sum(1))]
        return g, tip
    c = p.mean(0); X = p-c
    w, V = np.linalg.eigh(X.T @ X); ax = V[:, -1]
    pr = X @ ax
    a, b = p[np.argmax(pr)], p[np.argmin(pr)]
    def cam(q): return grip_point(q, fp)
    if np.linalg.norm(cam(a)) < np.linalg.norm(cam(b)): a, b = b, a
    # b — ближе к руке (рукоять)
    return b, a

def solve2(rot, pos, fp, tp, grip, tune, model):
    g, tip = axis(model, fp, grip)
    Rr = XYZ(rot)
    Ofp = Rr @ XYZ(fp.get('rotation',[0,0,0]))
    o = HAND + pos/16
    if grip is not None:
        G = grip_point(g, fp); P = o + G
        Tp = o + G + Rr @ (grip_point(tip, fp) - G)
    else:
        P = o + Rr @ grip_point(g, fp)
        Tp = o + Rr @ grip_point(tip, fp)
    Q = P + tune['lead']*(Tp-P)
    v = Q - SHOULDER
    v = v * np.array([tune['reach'], tune['reach'], 1])
    d = MF @ (v/np.linalg.norm(v))
    x = -math.acos(max(-1,min(1,d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-3 else 0
    A = Ry(y) @ Rx(x)
    W = np.linalg.inv(A @ C) @ (MF @ Ofp) @ np.linalg.inv(XYZ(tp.get('rotation',[0,0,0])))
    body = max(-D(tune['body_max']), min(D(tune['body_max']), y*tune['body']))
    return x, y, body, W, g

# ---------- общий вариант для обеих рук (m = 1 правая, -1 левая) ----------
def dR(disp, m):
    r = disp.get('rotation',[0,0,0]); return XYZ([r[0], m*r[1], m*r[2]])
def dT(disp, m):
    t = disp.get('translation',[0,0,0]); return np.array([m*t[0], t[1], t[2]])/16
def dS(disp):
    return np.array(disp.get('scale',[1,1,1]),float)
def gp(q, disp, m):  # точка модели (пиксели) → пространство руки при стойке
    return dR(disp, m) @ ((np.array(q)/16-0.5)*dS(disp)) + dT(disp, m)

def solve3(rot, pos, fp, tp, grip, handle, tip, tune, m):
    Rr = Rx(D(rot[0])) @ Ry(D(m*rot[1])) @ Rz(D(m*rot[2]))
    Ofp = Rr @ dR(fp, m)
    o = np.array([m*0.56, -0.52, -0.72]) + np.array([m*pos[0], pos[1], pos[2]])/16
    if grip is not None:
        G = gp(grip, fp, m)
        P = o + G + Rr @ (gp(handle, fp, m) - G)
        Tp = o + G + Rr @ (gp(tip, fp, m) - G)
    else:
        P = o + Rr @ gp(handle, fp, m); Tp = o + Rr @ gp(tip, fp, m)
    Q = P + tune['lead']*(Tp-P)
    S = np.array([m*5/16, -0.245, 0])
    v = (Q - S) * np.array([tune['reach'], tune['reach'], 1])
    d = MF @ (v/np.linalg.norm(v))
    x = -math.acos(max(-1,min(1,d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-3 else 0
    A = Ry(y) @ Rx(x)
    W = np.linalg.inv(A @ C) @ (MF @ Ofp) @ np.linalg.inv(dR(tp, m))
    return x, y, W, P

def tp_item_m(am, tp, handle, W, m, push=0):
    mm = am @ M4(R=C) @ T([m/16, .125, -.625])
    if push: mm = mm @ T([0, 0, -push/16])  # third.push — щит вперёд вдоль руки
    if W is not None:
        g = gp(handle, tp, m); mm = mm @ T(g) @ M4(R=W) @ T(-g)
    return mm @ T(dT(tp, m)) @ M4(R=dR(tp, m)) @ M4(s=dS(tp)) @ T([-.5,-.5,-.5])

def fp_item_m(fp, rot, pos, grip, m):
    mm = T([m*0.56, -0.52, -0.72]) @ T([m*pos[0]/16, pos[1]/16, pos[2]/16])
    R = Rx(D(rot[0])) @ Ry(D(m*rot[1])) @ Rz(D(m*rot[2]))
    if grip is not None:
        g = gp(grip, fp, m); mm = mm @ T(g) @ M4(R=R) @ T(-g)
    else: mm = mm @ M4(R=R)
    return mm @ T(dT(fp, m)) @ M4(R=dR(fp, m)) @ M4(s=dS(fp)) @ T([-.5,-.5,-.5])

def scene(arms, items, body_yaw):
    """arms: {'R':(x,y,z),'L':...}; items: {'R':(model, matrix_after_arm_fn)}"""
    q = []
    q += part_quads(WORLD @ part_m([0,0,0]), [-4,-8,-4], [8,8,8], (200,170,140))
    q += part_quads(WORLD @ part_m([0,0,0], 0, body_yaw), [-4,0,-2], [8,12,4], (60,90,150))
    for s in ('R','L'):
        sgn = 1 if s == 'R' else -1
        piv = [-5*math.cos(body_yaw)*sgn, 2, 5*math.sin(body_yaw)*sgn]
        x, y, z = arms[s]
        am = WORLD @ part_m(piv, x, y, z)
        q += part_quads(am, [-3 if s=='R' else -1,-2,-2], [4,12,4], (200,170,140) if s=='R' else (170,140,110))
        if s in items:
            model, fn = items[s]
            q += item_quads(model, fn(am), (190,40,50) if s=='R' else (150,30,90))
    for piv in ([-1.9,12,0],[1.9,12,0]):
        q += part_quads(WORLD @ part_m(piv), [-2,0,-2], [4,12,4], (50,60,90))
    return q

# ---------- с поправками (порт ThirdPerson.solve/base, 15 ноя) ----------
def hand_tip(fp, m, Rr, pos, grip, handle, tip):
    o = np.array([m*0.56, -0.52, -0.72]) + np.array([m*pos[0], pos[1], pos[2]])/16
    if grip is not None:
        G = gp(grip, fp, m)
        return o + G + Rr @ (gp(handle, fp, m) - G), o + G + Rr @ (gp(tip, fp, m) - G)
    return o + Rr @ gp(handle, fp, m), o + Rr @ gp(tip, fp, m)

def base4(disp, m, clip, t, tune, fix, grip, handle, tip):
    side = 'right' if m == 1 else 'left'
    fp = disp['firstperson_'+side+'hand']
    rot, pos = sample(clip, t)
    Rr = Rx(D(rot[0])) @ Ry(D(m*rot[1])) @ Rz(D(m*rot[2]))
    h, tp_ = hand_tip(fp, m, Rr, pos, grip, handle, tip)
    Q = h + tune['lead']*(tp_-h)
    if fix.get('lateral', 1) != 1:
        rh, rt = hand_tip(fp, m, np.eye(3), np.zeros(3), grip, handle, tip)
        rx = rh[0] + (rt[0]-rh[0])*tune['lead']
        Q[0] = rx + fix['lateral']*(Q[0]-rx)
    Q[0] *= fix.get('center', 1)
    if fix.get('no_cross'): Q[0] = m*max(m*Q[0], 0.08)
    v = (Q - np.array([m*5/16, -0.245, 0])) * np.array([tune['reach'], tune['reach'], 1])
    v = v/np.linalg.norm(v)
    d = np.array([-v[0], -v[1], v[2]])
    item = MF @ Rr @ dR(fp, m)
    if fix.get('follow_blade'):
        blade = item @ ((np.array(tip)-np.array(handle))*dS(fp))
        blade /= np.linalg.norm(blade)
        d = .25*d + .75*blade
        d /= np.linalg.norm(d)
    return d, item, h

SX = np.diag([-1.,1,1])
TURN_SIGN = -1  # turn: + — передние края щитов к центру
def angles(d):
    x = -math.acos(max(-1,min(1,d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-3 else 0
    return x, y

def rotation_between(a, b):
    """Rotation of the blade axis onto the forearm; mirrors ThirdPerson.followBlade."""
    a = np.array(a, float); a /= np.linalg.norm(a)
    b = np.array(b, float); b /= np.linalg.norm(b)
    cross = np.cross(a, b); dot = np.clip(a @ b, -1., 1.)
    if dot < -0.999999:
        helper = np.array([1., 0., 0.]) if abs(a[0]) < .9 else np.array([0., 1., 0.])
        axis = np.cross(a, helper); axis /= np.linalg.norm(axis)
        return 2 * np.outer(axis, axis) - np.eye(3)
    K = np.array([[0,-cross[2],cross[1]], [cross[2],0,-cross[0]], [-cross[1],cross[0],0]])
    return np.eye(3) + K + K @ K / (1. + dot)

def solve4(disp, m, clip, t, tune, fix, grip, handle, tip):
    side = 'right' if m == 1 else 'left'
    tp = disp['thirdperson_'+side+'hand']
    if fix.get('arm_space'):
        rot, pos = sample(clip, t)
        W = np.eye(3)
        if '_wrist_clip' in fix:
            roll = D(sample(fix['_wrist_clip'],t)[0][0])
            direction = gp(tip,tp,m)-gp(handle,tp,m);direction /= np.linalg.norm(direction)
            K = np.array([[0,-direction[2],direction[1]],[direction[2],0,-direction[0]],[-direction[1],direction[0],0]])
            W += math.sin(roll)*K+(1-math.cos(roll))*(K@K)
        x=VANILLA_ARM[0]+D(rot[0]);y=m*D(rot[1]);z=m*D(rot[2])
        hand = Rz(z)@Ry(y)@Rx(x)@C@(gp(handle,tp,m)+np.array([m/16,.125,-.625]))+np.array([-m*5/16,2/16,0])
        hand = np.array([-hand[0],-hand[1]-.119,hand[2]])
        return x, y, W, hand
    mirror = fix.get('mirror') and m == -1
    cm = 1 if mirror else m
    b = base4(disp, cm, clip, t, tune, fix, grip, handle, tip)
    bw = base4(disp, cm, clip, fix['wrist_hold'], tune, fix, grip, handle, tip) if fix.get('wrist_hold', -1) >= 0 else b
    if mirror:
        RY = np.eye(3) if fix.get('_keep_face') else Ry(math.pi)  # щиты: без переворота, лицо остаётся наружу
        b = (SX@b[0], SX@b[1]@SX@RY, SX@b[2]); bw = (SX@bw[0], SX@bw[1]@SX@RY, SX@bw[2])
    x, y = angles(b[0]); xw, yw = angles(bw[0])
    A = Ry(yw) @ Rx(xw)
    W = np.linalg.inv(A @ C) @ Ry(D(TURN_SIGN*m*fix.get('turn', 0))) @ bw[1] @ np.linalg.inv(dR(tp, m))  # turn — к центру
    return x, y, W, b[2]
