"""Reproducible D-021 previews using the existing first/third-person renderer.

python3 tools/preview/pale.py [output-directory]
The renderer shades the authored geometry; this is not an in-game texture/material capture.
"""
import json
import sys
from pathlib import Path
from tl import build, COMBO

ARCH = 'pale_linear_greatsword'

def generate(out, only=None):
    out = Path(out); out.mkdir(parents=True, exist_ok=True)
    combo = json.load(open(f'{COMBO}/{ARCH}.json'))
    solo = [(s['anim'], None, s['duration']/20) for s in combo['steps']]
    pair = [(s['anim'], s['off_anim'], s['duration']/20) for s in combo['dual_steps']]
    cases = {'solo': (solo, False), 'pair': (pair, True),
             'solo-r': ([('attack3', None, .44), ('ability', None, .38), ('rush', None, .6), ('recover', None, .4)], False),
             'pair-charge': ([('dual_charge', 'dual_charge_off', 1.5), ('dual_rush', 'dual_rush_off', .7), ('dual_recover', 'dual_recover_off', .35)], True),
             'pair-r': ([('dual_parry', 'dual_parry_off', .25), ('dual_repel', 'dual_repel_off', .45), ('dual_leap', 'dual_leap_off', .9), ('dual_slam', 'dual_slam_off', .7)], True),
             'left-r': ([(None, 'ability_off', .4), (None, 'rush_off', .6), (None, 'recover_off', .4)], True)}
    # R interrupting each individual attack, rather than only the third attack.
    for i, st in enumerate(solo, 1):
        cases[f'interrupt-{i}'] = ([(st[0], None, st[2]*.45), ('ability', None, .32), ('rush', None, .6)], False)
    if only and not only.issubset(cases):
        raise ValueError(f'Unknown preview cases: {sorted(only - cases.keys())}')
    for name, (steps, dual) in cases.items():
        if only and name not in only: continue
        frames = build(ARCH, steps, dual, str(out/f'{name}.gif'))
        # Filmstrip of the entire sequence, with each frame showing all three perspectives.
        from PIL import Image
        indices = [min(len(frames)-1, int(i*(len(frames)-1)/5)) for i in range(6)]
        sheet = Image.new('RGB', (1440, 720))
        for i, index in enumerate(indices): sheet.paste(frames[index], ((i%2)*720, (i//2)*240))
        sheet.save(out/f'{name}.png')
        print(name, len(frames), flush=True)
    # A short individual comparison for every base attack.
    for st in ([] if only else solo + pair):
        frames = build(ARCH, [st], st[1] is not None, str(out/f'{st[0]}.gif'))
        frames[len(frames)//3].save(out/f'{st[0]}.png')

if __name__ == '__main__':
    generate(sys.argv[1] if len(sys.argv) > 1 else 'out/pale', set(sys.argv[2:]) if len(sys.argv) > 2 else None)
