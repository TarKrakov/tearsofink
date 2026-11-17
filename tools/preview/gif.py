import sys, os, math
sys.path.insert(0, os.path.dirname(__file__))
from run import *
from PIL import ImageFont
FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', 13)
TWO = {'executioner', 'pale_linear_greatsword'}

def aim(P, m, tune):
    S = np.array([m*5/16, -0.245, 0])
    v = (P - S) * np.array([tune['reach'], tune['reach'], 1])
    d = MF @ (v/np.linalg.norm(v))
    x = -math.acos(max(-1,min(1,d[1])))
    y = math.atan2(-d[0], -d[2]) if abs(math.sin(x)) > 1e-3 else 0
    return x, y

def weight(c, t):
    w_in = min(1, t/0.1)
    out = c.get('return',0.3) if c.get('hold',0) > 0 else 0.15
    w_out = max(0, min(1, (end_time(c)-t)/out))
    return ease('ease_in_out', min(w_in, w_out))

def frame(arch, a, model, grip, c, t, both, label, tune):
    disp = model['display']
    rot, pos = sample(c, t)
    w = weight(c, t)
    arms = {'R': VANILLA_ARM, 'L': VANILLA_ARM}; items = {}; fq = []; body = 0
    handle, tip = axis(model, disp['firstperson_righthand'], grip)
    for s, m in (('R',1),('L',-1)):
        side = 'right' if m == 1 else 'left'
        fp = disp.get('firstperson_'+side+'hand'); tp = disp.get('thirdperson_'+side+'hand')
        if s == 'L' and not both:
            items_L = None
            continue
        x, y, W, P = solve3(rot, pos, fp, tp, grip, handle, tip, tune, m)
        if m == 1: Pmain = P
        v0 = VANILLA_ARM
        arms[s] = (v0[0]+(x-v0[0])*w, y*w, 0)
        Wq = slerp_m(W, w)
        items[s] = (model, (lambda tp, W, m: (lambda am: tp_item_m(am, tp, handle, W, m)))(tp, Wq, m))
        fq += item_quads(model, fp_item_m(fp, rot, pos, grip, m, view_offset=model.get('_fp_offset')), (190,40,50) if m == 1 else (150,30,90))
        if not both: body = max(-D(25), min(D(25), y*tune['body']))*w
    if not both and arch in TWO:
        x, y = aim(Pmain, -1, tune)
        v0 = VANILLA_ARM
        arms['L'] = (v0[0]+(x-v0[0])*w, y*w, 0)
    q = scene(arms, items, body)
    A_ = render(fq, [0,0,0], [0,0,-1], size=(240,180), fov=70, bg=(40,40,56))
    B_ = render(q, [0.9,2.4,2.6], [0,1.3,-0.6], size=(240,240), fov=50)
    C_ = render(q, [-1.8,1.9,-2.4], [0,1.25,0], size=(240,240), fov=50)
    im = Image.new('RGB', (720, 240), (20,20,20))
    im.paste(B_, (0,0)); im.paste(C_, (240,0)); im.paste(A_, (480,30))
    dr = ImageDraw.Draw(im)
    dr.text((4,2), 'сзади (F5)', fill=(200,200,200), font=FONT); dr.text((244,2), 'спереди', fill=(200,200,200), font=FONT)
    dr.text((484,2), 'от первого лица', fill=(200,200,200), font=FONT); dr.text((484,214), label, fill=(255,255,255), font=FONT)
    return im

def slerp_m(W, w):
    # поворот-матрица → ось-угол, доля w
    ang = math.acos(max(-1, min(1, (np.trace(W)-1)/2)))
    if ang < 1e-6: return np.eye(3)
    if abs(ang - math.pi) < 1e-4:
        # 180°: ось из собственного вектора
        vals, vecs = np.linalg.eig(W); ax = np.real(vecs[:, np.argmin(abs(vals-1))])
    else:
        ax = np.array([W[2,1]-W[1,2], W[0,2]-W[2,0], W[1,0]-W[0,1]])/(2*math.sin(ang))
    ax /= np.linalg.norm(ax); a = ang*w
    K = np.array([[0,-ax[2],ax[1]],[ax[2],0,-ax[0]],[-ax[1],ax[0],0]])
    return np.eye(3)+math.sin(a)*K+(1-math.cos(a))*K@K

def gif(arch, seq, out, fps=20, tune=None):
    tune = tune or dict(TUNE)
    a, model, fp, tp, grip = load(arch)
    frames = []
    for nm in seq:
        c = a['animations'][nm]
        both = nm.startswith('dual') or nm == 'cross_burst'
        tempo = 1.0
        T_ = end_time(c); n = max(2, int(T_*fps))
        for i in range(n):
            t = T_*i/n
            frames.append(frame(arch, a, model, grip, c, t, both, f'{nm}  {t:.2f}s', tune))
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=int(1000/fps), loop=0)

if __name__ == '__main__':
    gif(sys.argv[1], sys.argv[2].split(','), sys.argv[3])
