import sys, os, json, copy
sys.path.insert(0, os.path.dirname(__file__))
from tl import *

def pair(arch, main, off, times, fix=None, out='p.png'):
    tune = dict(TUNE)
    a, model, fp0, tp0, grip = load(arch)
    disp = model['display']; anims = a['animations']
    cm = copy.deepcopy(anims[main]); co = copy.deepcopy(anims[off])
    if fix is not None: cm['third'] = fix; co['third'] = fix
    cols = []
    for t in times:
        arms = {}; items = {}; fq = []
        for s, m, c in (('R',1,cm),('L',-1,co)):
            arm, W, tp, handle, q, by, P, w = hand_pose(arch, model, grip, c, t, m, tune)
            arms[s] = arm
            items[s] = (model, (lambda tp, W, m, handle: (lambda am: tp_item_m(am, tp, handle, W, m)))(tp, W, m, handle))
            fq += q
        qq = scene(arms, items, 0)
        A_ = render(fq, [0,0,0], [0,0,-1], size=(220,160), fov=70, bg=(40,40,56))
        B_ = render(qq, [0.9,2.4,2.6], [0,1.3,-0.6], size=(220,220), fov=50)
        C_ = render(qq, [-1.4,1.9,-2.8], [0,1.25,0], size=(220,220), fov=50)
        im = Image.new('RGB', (220, 600), (20,20,20))
        im.paste(A_, (0,0)); im.paste(B_, (0,160)); im.paste(C_, (0,380))
        ImageDraw.Draw(im).text((4,2), f'{main} {t:.2f}', fill=(255,255,255), font=FONT)
        cols.append(im)
    img = Image.new('RGB', (220*len(cols), 600))
    for i, c in enumerate(cols): img.paste(c, (i*220, 0))
    img.save(out); return img

def stack(imgs, out):
    W = max(i.size[0] for i in imgs); H = sum(i.size[1] for i in imgs)
    o = Image.new('RGB', (W, H)); y = 0
    for i in imgs: o.paste(i, (0, y)); y += i.size[1]
    o.save(out)
