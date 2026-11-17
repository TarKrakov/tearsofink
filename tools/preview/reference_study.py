"""Read-only motion study of the six accepted weapon archetypes.
Run from repository root: python3 tools/preview/reference_study.py [--render]
"""
import argparse
import json
from pathlib import Path
from PIL import Image
from tl import build, COMBO
from run import load

REFERENCES = ('axe', 'dagger', 'executioner', 'scimitar', 'talwar', 'dussak')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--render', action='store_true')
    parser.add_argument('--out', default='out/reference-study')
    args = parser.parse_args()
    out = Path(args.out); out.mkdir(parents=True, exist_ok=True)
    report = {}
    for arch in REFERENCES:
        combo = json.loads((Path(COMBO)/f'{arch}.json').read_text())
        animations = load(arch)[0]['animations']
        report[arch] = []
        steps = []
        for step in combo['steps']:
            clip = animations[step['anim']]
            report[arch].append(dict(clip=step['anim'], impact=clip.get('impact'),
                curve=clip.get('smooth'), keys=clip['keyframes'], duration=step['duration']/20,
                hold=clip.get('hold',0), recovery=clip.get('return',.3)))
            steps.append((step['anim'],None,step['duration']/20/combo.get('tempo',1)))
        if args.render:
            frames = build(arch, steps, False, str(out/f'{arch}.gif'), fps=30)
            sheet = Image.new('RGB',(2160,960))
            for i in range(12):
                sheet.paste(frames[round(i*(len(frames)-1)/11)],(i%3*720,i//3*240))
            sheet.save(out/f'{arch}.png')
        print(arch, len(steps), 'steps')
    (out/'motion.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')

if __name__ == '__main__':
    main()
