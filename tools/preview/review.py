"""Model-aware review of animation clips; uses Claude's existing 3D math.

    python3 tools/preview/review.py pale_linear_greatsword --check-only
    python3 tools/preview/review.py pale_linear_greatsword --render

Warnings are diagnostics, NOT artistic approval or gameplay testing.
"""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from run import load
from third import C, apply4, axis, dR, end_time, fp_item_m, model_points, sample, solve4
from tl import COMBO, build

FOV = math.tan(math.radians(70 / 2))
ASPECT = 240 / 180


def preview_pose(anims, clip):
    """Retain the original clip's third-person settings when sourcing F5 keys."""
    fix = dict(clip.get('third', {}))
    if fix.get('wrist_source'): fix['_wrist_clip'] = anims[fix['wrist_source']]
    src = fix.get('source')
    if src is not None and src not in anims:
        raise ValueError(f'Unknown third.source: {src}')
    return anims[src] if src else clip, fix


def frame_data(model, grip, blade_tip, vertices, clip, t, hand):
    side = 'right' if hand == 1 else 'left'
    display = model['display']['firstperson_' + side + 'hand']
    rot, pos = sample(clip, t)
    mat = fp_item_m(display, rot, pos, grip, hand)
    points = apply4(mat, vertices)
    depth = -points[:, 2]
    with np.errstate(divide='ignore', invalid='ignore'):
        visible = ((depth > .05) &
                   (abs(points[:, 0] / depth) < FOV * ASPECT) &
                   (abs(points[:, 1] / depth) < FOV))
    tip = apply4(mat, np.array([blade_tip]) / 16)[0]
    xy = np.array([np.nan, np.nan]) if tip[2] >= -.05 else tip[:2] / -tip[2]
    return float(np.mean(visible)), xy


def inspect(arch, samples=50):
    a, model, fp, _, grip = load(arch)
    combo = json.loads((Path(COMBO) / (arch + '.json')).read_text(encoding='utf-8'))
    anims = a['animations']
    hilt, tip = axis(model, fp, grip)
    vertices = model_points(model) / 16
    selected = []
    for step in combo.get('steps', []):
        selected.append((step['anim'], 1, step['hit'] / 20, step.get('second_hit')))
    for step in combo.get('dual_steps', []):
        if step.get('anim'):
            selected.append((step['anim'], 1, step['hit'] / 20, step.get('second_hit')))
        if step.get('off_anim'):
            selected.append((step['off_anim'], -1, step['hit'] / 20, step.get('second_hit')))
    for name in ('dual_charge', 'dual_charge_off', 'dual_parry', 'dual_parry_off'):
        if name in anims:
            selected.append((name, -1 if name.endswith('_off') else 1, None, None))
    seen, findings, warnings = set(), [], []
    for name, hand, impact, second_hit in selected:
        if (name, hand) in seen:
            continue
        seen.add((name, hand))
        if name not in anims:
            raise ValueError(f'Missing animation {name} for {arch}')
        clip = anims[name]
        keytimes = [key['t'] for key in clip.get('keyframes', [])]
        if not keytimes or any(x >= y for x, y in zip(keytimes, keytimes[1:])):
            raise ValueError(f'Unsorted or empty keyframes: {name}')
        if impact is not None:
            candidates = [impact] + ([second_hit / 20] if second_hit is not None else [])
            actual = clip.get('impact', -1)
            if not any(abs(actual - candidate) <= .035 for candidate in candidates):
                raise ValueError(f'Combat hit marker differs from animation impact: {name}')
            impact = actual
            if impact > end_time(clip):
                raise ValueError(f'Combat hit occurs after animation ends: {name}')
        timeline = np.linspace(0, max(keytimes), samples)
        if impact is not None:
            timeline = np.unique(np.r_[timeline, impact])
        frames = [frame_data(model, grip, tip, vertices, clip, float(t), hand) for t in timeline]
        fractions = np.array([x[0] for x in frames])
        xy = np.array([x[1] for x in frames])
        if not np.isfinite(xy).all():
            raise ValueError(f'Blade behind camera during {name}')
        steps = np.linalg.norm(np.diff(xy, axis=0), axis=1)
        velocities = steps / np.diff(timeline)
        impact_fraction = None
        if impact is not None:
            impact_fraction = frame_data(model, grip, tip, vertices, clip, impact, hand)[0]
            if impact_fraction < .85:
                warnings.append(f'{name}: only {impact_fraction:.0%} of mesh visible on hit')
        if len(steps) and np.max(steps) > .2:
            warnings.append(f'{name}: unusually large per-sample tip movement; inspect curve')
        # Compare one-sided tangents near every keyframe. Large changes are
        # a likely source of visible snapping, even with smooth interpolation.
        max_corner = 0.0
        for previous, keytime, following in zip(keytimes[:-2], keytimes[1:-1], keytimes[2:]):
            dt = min(.012, .25 * (keytime - previous), .25 * (following - keytime))
            left = frame_data(model, grip, tip, vertices, clip, keytime-dt, hand)[1]
            middle = frame_data(model, grip, tip, vertices, clip, keytime, hand)[1]
            right = frame_data(model, grip, tip, vertices, clip, keytime+dt, hand)[1]
            jump = float(np.linalg.norm((right - middle) / dt - (middle - left) / dt))
            max_corner = max(max_corner, jump)
        if max_corner > 6:
            warnings.append(f'{name}: abrupt keyframe velocity change ({max_corner:.1f}); inspect blending')
        f5_clip, fix = preview_pose(anims, clip)
        f5_alignment = None
        if fix.get('follow_blade'):
            tp = model['display']['thirdperson_' + ('right' if hand == 1 else 'left') + 'hand']
            _, _, wrist, _ = solve4(model['display'], hand, f5_clip, min(.3, max(keytimes)),
                                    a.get('third', {}), fix, grip, hilt, tip)
            blade = C @ wrist @ dR(tp, hand) @ (tip - hilt)
            blade /= np.linalg.norm(blade)
            forearm = np.array([0., 1., -.15])
            forearm /= np.linalg.norm(forearm)
            f5_alignment = float(blade @ forearm)
            if f5_alignment < math.cos(math.pi/4)-1e-5:
                raise ValueError(f'{name}: F5 sword is not aligned to the carrying forearm')
        findings.append({
            'clip': name, 'hand': 'left' if hand == -1 else 'right',
            'frames_checked': int(len(timeline)),
            'lowest_visible_fraction': round(float(np.min(fractions)), 3),
            'impact_visible_fraction': None if impact_fraction is None else round(impact_fraction, 3),
            'largest_tip_step': round(float(np.max(steps)), 3) if len(steps) else 0,
            'highest_tip_speed': round(float(np.max(velocities)), 3) if len(velocities) else 0,
            'largest_keyframe_velocity_change': round(max_corner, 3),
            'f5_blade_arm_alignment': None if f5_alignment is None else round(f5_alignment, 5),
        })
    return combo, {
        'archetype': arch, 'clips': findings, 'warnings': warnings,
        'important': 'Mathematical preview, not Minecraft playtesting or artistic approval'
    }


def render(arch, combo, out, fps):
    out.mkdir(parents=True, exist_ok=True)
    tempo = combo.get('tempo', 1)
    cases = {
        'solo': ([(s['anim'], None, s['duration'] / (20 * tempo)) for s in combo.get('steps', [])], False),
        'dual': ([(s['anim'], s.get('off_anim'), s['duration'] / (20 * tempo)) for s in combo.get('dual_steps', [])], True),
    }
    a, _, _, _, _ = load(arch)
    if 'dual_charge' in a['animations'] and 'dual_charge_off' in a['animations']:
        cases['charge'] = ([('dual_charge', 'dual_charge_off', 1.5)], True)
    from PIL import Image
    for label, (steps, two_hands) in cases.items():
        if not steps:
            continue
        frames = build(arch, steps, two_hands, str(out / (label + '.gif')), fps=fps, front=(arch == "pale_linear_greatsword"))
        count = min(12, len(frames))
        contact = Image.new('RGB', (720 * 3, 240 * 4), (20, 20, 20))
        for i, idx in enumerate(np.linspace(0, len(frames) - 1, count).astype(int)):
            contact.paste(frames[idx], ((i % 3) * 720, (i // 3) * 240))
        contact.save(out / (label + '.png'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archetype')
    parser.add_argument('--out', default='out/review')
    parser.add_argument('--samples', type=int, default=50)
    parser.add_argument('--render', action='store_true', help='Produce GIFs and contact sheets in three views')
    parser.add_argument('--check-only', action='store_true', help='Validate without rendering')
    parser.add_argument('--fps', type=int, default=30)
    args = parser.parse_args()
    if args.samples < 8 or args.fps < 1:
        parser.error('samples must be at least 8; fps must be positive')
    combo, report = inspect(args.archetype, args.samples)
    out = Path(args.out) / args.archetype
    out.mkdir(parents=True, exist_ok=True)
    (out / 'report.json').write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Reviewed {len(report['clips'])} clips for {args.archetype}; warnings: {len(report['warnings'])}")
    for issue in report['warnings']:
        print('REVIEW:', issue)
    if args.render and not args.check_only:
        render(args.archetype, combo, out, args.fps)
        print('Preview GIFs + contact sheets:', out)
    print('Report:', out / 'report.json')


if __name__ == '__main__':
    main()
