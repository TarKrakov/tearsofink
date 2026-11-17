import sys, os, json, math
sys.path.insert(0, os.path.dirname(__file__))
from gif import *

COMBO = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../src/main/resources/data/tears-of-ink/weapon_combos')

def hand_pose(arch, model, grip, c, t, m, tune, continuing=False):
    disp = model['display']; side = 'right' if m == 1 else 'left'
    fp = disp['firstperson_'+side+'hand']; tp = disp['thirdperson_'+side+'hand']
    if tune.get('centre'):  # щит — за середину
        pts = model_points(model); cc = (pts.min(0)+pts.max(0))/2; handle, tip = cc, cc+np.array([0,8,0])
    else:
        handle, tip = axis(model, disp['firstperson_righthand'], grip)
    rot, pos = sample(c, t); w = weight(c, t)
    if continuing: w = ease("ease_in_out", min(1, max(0, (end_time(c)-t)/(c.get("return", .3) if c.get("hold", 0) > 0 else .15))))
    fx = dict(c.get('third', {}))  # correction belongs to the original clip, not its F5 source
    src = fx.get('source')
    if src: c = dict(c, keyframes=json.load(open(f'{REPO}/weapon_anims/{arch}.json'))['animations'][src]['keyframes'])  # третье лицо — по другому движению
    if tune.get('centre'): fx['_keep_face'] = True
    x, y, W, P = solve4(disp, m, c, t, tune, fx, grip, handle, tip)
    v0 = VANILLA_ARM
    arm = (v0[0]+(x-v0[0])*w, y*w, 0)
    fq = item_quads(model, fp_item_m(fp, rot, pos, grip, m), (190,40,50) if m == 1 else (150,30,90))
    return arm, slerp_m(W, w), tp, handle, fq, y*w, P, w

def build(arch, steps, dual, out, fps=20, tune=None):
    a, model, fp0, tp0, grip = load(arch)
    tune = tune or dict(TUNE, **a.get("third", {}))
    combo = json.load(open(f'{COMBO}/{arch}.json')); tempo = combo.get('tempo', 1.0)
    anims = a['animations']; disp = model['display']
    # расписание: (время старта, рука, движение)
    sched = []; T = 0.0
    for st in steps:
        main, off = st[0], st[1]
        if main: sched.append((T, 1, main))
        if off: sched.append((T, -1, off))
        dur = st[2] if len(st) > 2 else 0.6
        T += dur
    # Capture both displayed poses at each interruption, including an unfinished previous blend.
    # Same 0.12 real-second ease_out as WeaponAnims.shown / ThirdPerson.apply.
    states = {}
    def pose(state, now, m):
        start, nm, previous = state
        c = anims[nm]; t = (now-start)*tempo
        if t >= end_time(c): return None
        result = list(hand_pose(arch, model, grip, c, t, m, tune, previous is not None))
        rot, pos = sample(c, t)
        if previous is None and arch == 'pale_linear_greatsword' and c.get('smooth') and now-start < .20:
            u = ease('ease_out', max(0, (now-start)/.20))
            rot, pos = rot*u, pos*u
        if previous is not None and now-start < (.20 if arch == 'pale_linear_greatsword' else .12):
            u = ease('ease_out', max(0, (now-start)/(.20 if arch == 'pale_linear_greatsword' else .12)))
            if c.get('smooth'):
                rot = previous[1][0]*(1-u) + rot*u
                pos = previous[1][1]*(1-u) + pos*u
            result[0] = tuple(np.array(previous[0][0])*(1-u) + np.array(result[0])*u)
            W0, W1 = previous[0][1], result[1]
            result[1] = W0 @ slerp_m(W0.T @ W1, u)
        fp = disp['firstperson_'+('right' if m == 1 else 'left')+'hand']
        result[4] = item_quads(model, fp_item_m(fp, rot, pos, grip, m), (190,40,50) if m == 1 else (150,30,90))
        return result, (rot, pos)
    events = []
    for start, m, nm in sched:
        old = states.get(m)
        previous = pose(old, start, m) if old else None
        state = (start, nm, previous)
        states[m] = state; events.append((start, m, state))
    total = T + 0.8
    frames = []
    for i in range(int(total*fps)):
        now = i/fps
        arms = {'R': VANILLA_ARM, 'L': VANILLA_ARM}; items = {}; fq = []; body = 0; labels = []
        active = {}
        for st_t, m, state in events:
            if st_t <= now: active[m] = state
        Pmain = None; wmain = 0
        for m, state in active.items():
            sampled = pose(state, now, m)
            if sampled is None: continue
            nm = state[1]
            arm, W, tp, handle, q, by, P, w = sampled[0]
            s = 'R' if m == 1 else 'L'
            arms[s] = arm
            items[s] = (model, (lambda tp, W, m, handle: (lambda am: tp_item_m(am, tp, handle, W, m, tune.get('push', 0))))(tp, W, m, handle))
            fq += q; body += by*tune['body']; labels.append(nm)
            if m == 1: Pmain, wmain = P, w
        if not dual and arch in TWO and Pmain is not None:
            x, y = aim(Pmain*np.array([0.6,1,1]), -1, tune); v0 = VANILLA_ARM
            arms['L'] = (v0[0]+(x-v0[0])*wmain, y*wmain, 0)
        for s, m in (('R',1),('L',-1)):
            if s not in items and (dual or s == 'R'):
                side = 'right' if m == 1 else 'left'
                tp = disp['thirdperson_'+side+'hand']
                items[s] = (model, (lambda tp, m: (lambda am: tp_item_m(am, tp, None, None, m)))(tp, m))
                fq += item_quads(model, fp_item_m(disp['firstperson_'+side+'hand'], np.zeros(3), np.zeros(3), grip, m), (190,40,50) if m == 1 else (150,30,90))
        if len(labels) > 1: body = 0
        body = max(-D(25), min(D(25), body))
        q = scene(arms, items, body)
        A_ = render(fq, [0,0,0], [0,0,-1], size=(240,180), fov=70, bg=(40,40,56))
        B_ = render(q, [0.9,2.4,2.6], [0,1.3,-0.6], size=(240,240), fov=50)
        C_ = render(q, [-1.8,1.9,-2.4], [0,1.25,0], size=(240,240), fov=50)
        im = Image.new('RGB', (720, 240), (20,20,20))
        im.paste(B_, (0,0)); im.paste(C_, (240,0)); im.paste(A_, (480,30))
        dr = ImageDraw.Draw(im)
        dr.text((4,2), 'сзади (F5)', fill=(200,200,200), font=FONT); dr.text((244,2), 'спереди', fill=(200,200,200), font=FONT)
        dr.text((484,2), 'от первого лица', fill=(200,200,200), font=FONT)
        dr.text((484,214), f'{arch}: ' + (' + '.join(labels) or '—'), fill=(255,255,255), font=FONT)
        dr.text((4,220), f'{now:.2f} s', fill=(255,255,255), font=FONT)
        frames.append(im)
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=[(round((i+1)*100/fps)-round(i*100/fps))*10 for i in range(len(frames))], loop=0)
    return frames

def all_for(arch):
    combo = json.load(open(f'{COMBO}/{arch}.json'))
    solo = [(s['anim'], None, s['duration']/20/combo.get('tempo', 1)) for s in combo['steps']]
    solo.append((combo['ability_anim'], None, combo.get('ability_ticks', 20)/20+0.3))
    dual = [(s.get('anim') or None, s.get('off_anim') or None, s['duration']/20/combo.get('tempo', 1)) for s in combo['dual_steps']]
    # hold «late»: шаги идут сразу друг за другом
    build(arch, solo, False, f'out/{arch}_solo.gif')
    build(arch, dual, True, f'out/{arch}_dual.gif')

if __name__ == '__main__':
    os.makedirs('out', exist_ok=True)
    for a in sys.argv[1:]: all_for(a)
