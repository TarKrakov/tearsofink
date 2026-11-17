"""Reproducible D-021 previews using the existing first/third-person renderer.

python3 tools/preview/pale.py [output-directory]
The renderer shades the authored geometry; this is not an in-game texture/material capture.
"""
import json
import hashlib
import sys
from pathlib import Path
from tl import build, COMBO

ARCH = 'pale_linear_greatsword'

def generate(out, only=None):
    out = Path(out); out.mkdir(parents=True, exist_ok=True)
    root=Path(__file__).resolve().parents[2]
    inputs=['src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword.json',
            'src/main/resources/assets/tears-of-ink/models/item/pale_linear_greatsword.json',
            'src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.json']
    inputs += ['tools/preview/'+n+'.py'for n in('pale','tl','third','run','gif')]
    def snapshot():return {name:hashlib.sha256((root/name).read_bytes()).hexdigest()for name in inputs}
    before=snapshot()
    combo = json.load(open(f'{COMBO}/{ARCH}.json'))
    solo = [(s['anim'], None, s['duration']/20) for s in combo['steps']]
    pair = [(s.get('anim') or None, s.get('off_anim') or None, s['duration']/20) for s in combo['dual_steps']]
    cases = {'baseline': ([(None,None,.1)], True), 'solo': (solo, False), 'pair': (pair, True),
             'solo-r': ([(solo[-1][0], None, .25), ('ability', None, .38), ('rush', None, .6), ('recover', None, .4)], False),
             'pair-charge': ([('dual_charge', 'dual_charge_off', 1.5), ('dual_rush', 'dual_rush_off', .7), ('dual_recover', 'dual_recover_off', .35)], True),
             'solo-backstep': ([('backstep',None,.6)],False),
             'solo-guard': ([('guard',None,1.5),('guard_hold',None,1.5),('guard_release',None,.3)],False),
             'dual-guard': ([('dual_guard','dual_guard_off',1.5),('dual_guard_hold','dual_guard_hold_off',1.5),('guard_release','guard_release_off',.3)],True),
             'parry-cross': ([('dual_parry','dual_parry_off',.9)],True),
             'pair-r': ([('dual_parry', 'dual_parry_off', .65), ('dual_repel', 'dual_repel_off', .45), ('dual_leap', 'dual_leap_off', .9), ('dual_slam', 'dual_slam_off', .7)], True),
             'left-r': ([(None, 'ability_off', .4), (None, 'rush_off', .6), (None, 'recover_off', .4)], True)}
    # R interrupting each individual attack, rather than only the third attack.
    for i, st in enumerate(solo, 1):
        cases[f'interrupt-{i}'] = ([(st[0], None, st[2]*.45), ('ability', None, .32), ('rush', None, .6)], False)
    if only and not only.issubset(cases):
        raise ValueError(f'Unknown preview cases: {sorted(only - cases.keys())}')
    for name, (steps, dual) in cases.items():
        if only and name not in only: continue
        frames = build(ARCH, steps, dual, str(out/f'{name}.gif'), fps=30, front=True, widescreen=True, fp_out=str(out/f'{name}-fp.gif') if name in ('solo','pair','pair-charge','solo-backstep','pair-r','solo-guard','dual-guard') else None)
        # Filmstrip of the entire sequence, with each frame showing all three perspectives.
        from PIL import Image
        indices = [min(len(frames)-1, int(i*(len(frames)-1)/5)) for i in range(6)]
        phases={'pair-r':[0,.35,.80,1.,1.55,2.2], 'pair-charge':[0,.5,1.35,1.5,1.62,2.2]}
        if name in phases: indices=[min(len(frames)-1,round(t*30)) for t in phases[name]]
        sheet = Image.new('RGB', (1440, 720))
        for i, index in enumerate(indices): sheet.paste(frames[index], ((i%2)*720, (i//2)*240))
        sheet.save(out/f'{name}.png')
        if name=='pair':
            # Inspect the actual shared attacks, including the preceding alternating handoff.
            focus=frames[round(.8*30):round(1.6*30)]
            focus[0].save(out/'pair-3-4.gif',save_all=True,append_images=focus[1:],duration=[(round((i+1)*100/30)-round(i*100/30))*10 for i in range(len(focus))],loop=0)
            contact=Image.new('RGB',(1440,960))
            for i,t in enumerate((.8,.92,1.,1.1,1.2,1.32,1.4,1.5)):
                contact.paste(frames[min(len(frames)-1,round(t*30))],((i%2)*720,(i//2)*240))
            contact.save(out/'pair-3-4.png')
        if name in ('solo','pair','pair-charge','solo-backstep','pair-r','solo-guard','dual-guard'):
            from PIL import ImageSequence
            fps_frames=[f.convert('RGB')for f in ImageSequence.Iterator(Image.open(out/f'{name}-fp.gif'))]
            moments={'solo':(.10,.23,.37,.70,.80,1.37,1.47,1.60),
                     'pair':(.08,.20,.6,.92,1.,1.32,1.4,2.06),
                     'pair-charge':(0,.5,1.,1.35,1.5,1.62,1.85,2.2),
                     'solo-backstep':(0,.07,.13,.17,.23,.33,.5,.7),
                     'solo-guard':(0,.12,.26,.6,1.6,2.95,3.15,3.5),
                     'dual-guard':(0,.12,.26,.6,1.6,2.95,3.15,3.5),
                     'pair-r':(.65,1.0,1.1,1.3,1.55,1.75,2.0,2.2)}[name]
            large=Image.new('RGB',(1920,540))
            for i,t in enumerate(moments):
                large.paste(fps_frames[min(len(fps_frames)-1,round(t*30))].resize((480,270)),((i%4)*480,(i//4)*270))
            large.save(out/f'{name}-fp.png')
            if name=='pair':
                focus=fps_frames[24:48]
                focus[0].save(out/'pair-3-4-fp.gif',save_all=True,append_images=focus[1:],duration=[(round((i+1)*100/30)-round(i*100/30))*10 for i in range(len(focus))],loop=0)
        print(name, len(frames), flush=True)
    # A short individual comparison for every base attack.
    for index, st in enumerate([] if only else solo + pair):
        label=st[0] if index<len(solo) else f'lmb-pair-{index-len(solo)+1}'
        frames = build(ARCH, [st], index>=len(solo), str(out/f'{label}.gif'), fps=30, front=True, widescreen=True)
        # Dense impact sheet; no sparse 1/3-time snapshot hiding the active stroke.
        import json as _json
        from third import REPO
        name=st[0] or st[1]
        clip=_json.load(open(f'{REPO}/weapon_anims/{ARCH}.json'))['animations'][name]
        impact=clip['impact']
        times=[0,max(0,impact-.12),max(0,impact-.06),impact,impact+.06,impact+.12,st[2],(len(frames)-1)/30]
        sheet=Image.new('RGB',(1440,960))
        for i,t in enumerate(times):
            sheet.paste(frames[min(len(frames)-1,round(t*30))],((i%2)*720,(i//2)*240))
        sheet.save(out/f'{label}.png')

    assert snapshot()==before,'Preview inputs changed during rendering; rerun into a clean output directory'
    (out/'preview-inputs.json').write_text(json.dumps({'inputs':before,'fps':30,'fp_aspect':'16:9','fp_resolution':[960,540],'complete':True,'cases':sorted(only or cases)},indent=2)+'\n')

if __name__ == '__main__':
    generate(sys.argv[1] if len(sys.argv) > 1 else 'out/pale', set(sys.argv[2:]) if len(sys.argv) > 2 else None)
