import json, sys, math, os
import numpy as np
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from third import *

FP_FOV = 70
ITEM = {'executioner':'spire_torturer_greatsword','axe':'spire_roaring_axe','dagger':'spire_rapid_knife','scimitar':'spire_shadow_scimitar','talwar':'blood_talvar','dussak':'blood_dussak','pale_linear_greatsword':'pale_linear_great'}
TUNE = dict(reach=1.3, body=0.3, body_max=25, lead=0.5)

def resolve_model(item_name, seen=None):
    """Resolve authored parent models, preserving child display transforms.

    New weapons do not need hardcoded entries in ITEM if their registered
    item JSON points to a 3D Blockbench parent model.
    """
    seen = set() if seen is None else seen
    relative = item_name.split(':', 1)[-1]
    if relative.startswith('item/'):
        relative = relative[5:]
    path = f'{REPO}/models/item/{relative}.json'
    if path in seen:
        raise ValueError(f'Cyclic item model parent: {path}')
    seen.add(path)
    with open(path, encoding='utf-8') as source:
        local = json.load(source)
    if 'elements' in local:
        return local
    parent = local.get('parent', '')
    if not parent or parent.startswith('minecraft:'):
        raise ValueError(f'{path}: 3D Blockbench geometry is unavailable')
    base = resolve_model(parent, seen)
    base['display'] = {**base.get('display', {}), **local.get('display', {})}
    return base


def load(arch):
    with open(f'{REPO}/weapon_anims/{arch}.json', encoding='utf-8') as source:
        a = json.load(source)
    m = resolve_model(ITEM.get(arch, arch))
    # Backward-compatible aliases (including original authored left-hand poses).
    alias_path = f'{REPO}/models/item/{arch}.json'
    if arch in ITEM and os.path.exists(alias_path):
        with open(alias_path, encoding='utf-8') as source:
            overlay = json.load(source)
        m['display'] = {**m.get('display', {}), **overlay.get('display', {})}
    m['_fp_offset']=a.get('firstperson_offset',[0,0,0])
    disp = m['display']
    missing = [name for name in ('firstperson_righthand', 'firstperson_lefthand',
                                 'thirdperson_righthand', 'thirdperson_lefthand')
               if name not in disp]
    if missing:
        raise ValueError(f'{arch}: add authored display transforms for {missing}')
    return a, m, disp['firstperson_righthand'], disp['thirdperson_righthand'], a.get('grip')

def cell(arch, clipname, t, model, disp, grip, clip, both, tune):
    rot, pos = sample(clip, t)
    arms = {'R': VANILLA_ARM, 'L': VANILLA_ARM}; items = {}; fq = []; body = 0
    handle, tip = axis(model, disp['firstperson_righthand'], grip)
    for s, m in (('R',1),('L',-1)):
        if s == 'L' and not both: continue
        side = 'right' if m == 1 else 'left'
        fp = disp.get('firstperson_'+side+'hand', disp['firstperson_righthand'])
        tp = disp.get('thirdperson_'+side+'hand', disp['thirdperson_righthand'])
        x, y, W, P = solve3(rot, pos, fp, tp, grip, handle, tip, tune, m)
        arms[s] = (x, y, 0)
        items[s] = (model, (lambda tp, W, m: (lambda am: tp_item_m(am, tp, handle, W, m)))(tp, W, m))
        fq += item_quads(model, fp_item_m(fp, rot, pos, grip, m, view_offset=model.get('_fp_offset')), (190,40,50) if m == 1 else (150,30,90))
        if not both: body = max(-D(tune['body_max']), min(D(tune['body_max']), y*tune['body']))
    q = scene(arms, items, body)
    a = render(fq, [0,0,0], [0,0,-1], size=(200,150), fov=FP_FOV, bg=(40,40,56))
    b = render(q, [0.9,2.4,2.6], [0,1.3,-0.6], size=(200,200), fov=50)
    c = render(q, [-1.6,1.9,-2.6], [0,1.25,0], size=(200,200), fov=50)
    im = Image.new('RGB', (200, 550), (20,20,20))
    im.paste(a, (0,0)); im.paste(b, (0,150)); im.paste(c, (0,350))
    ImageDraw.Draw(im).text((4,2), f'{clipname} t={t:.2f}', fill=(255,255,255))
    return im

def sheet(arch, clips=None, n=6, out=None, tune=TUNE):
    a, model, fp, tp, grip = load(arch)
    disp = model['display']
    anims = a['animations']
    names = clips or list(anims)
    rows = []
    for nm in names:
        c = anims[nm]
        end = end_time(c)
        ts = [end*i/(n-1)*0.999 for i in range(n)]
        both = nm.startswith('dual') or nm == 'cross_burst'
        rows.append([cell(arch, nm, t, model, disp, grip, c, both, tune) for t in ts])
    W_, H_ = 200, 550
    img = Image.new('RGB', (W_*n, H_*len(rows)), (0,0,0))
    for r, row in enumerate(rows):
        for i, ce in enumerate(row): img.paste(ce, (i*W_, r*H_))
    img.save(out or f'/home/claude/preview/{arch}.png')

if __name__ == '__main__':
    arch = sys.argv[1]; clips = sys.argv[2].split(',') if len(sys.argv) > 2 else None
    os.makedirs('out', exist_ok=True)
    sheet(arch, clips, out=f'out/{arch}_{"_".join(clips) if clips else "all"}.png')
