#!/usr/bin/env python3
"""Bake the TOI Blockbench rig into new, opt-in WeaponAnims clips (seconds/pixels/degrees)."""
import argparse
import difflib
import hashlib
import json
import math
import sys
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/preview'))
from run import load
from third import C, WORLD, VANILLA_ARM, XYZ, Rx, Ry, Rz, dR, gp
from build_workspace import euler_zyx
from hilt import euler_xyz

class ImportError(ValueError):
    pass

# Exact, reviewed migration of the old V1 source: author's 856d3ff save contains
# the original 31 cubes followed by an identical copy using the second,
# byte-identical texture. Display, UV, pivots and exported Java model are unchanged.
# Pin BOTH the original calibrated digest and the replacement Git blob. No
# arbitrary changes to an author's model bypass the calibration checks.
V1_SOURCE_MIGRATIONS = {
    ('src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel',
     'a336122c49a19367ba7ad0a30f97b8afbb13fae65695190facc3048eada51593'):
        ('69b513c44dbf980542be42b6f13186b2e60538fc', 31),
}

def calibrated_source(path, expected, version):
    data = (ROOT/path).read_bytes()
    if hashlib.sha256(data).hexdigest() == expected:
        return None
    migration = V1_SOURCE_MIGRATIONS.get((path, expected)) if version == 1 else None
    if migration:
        blob = hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()
        if blob == migration[0]:
            return migration[1]
    raise ImportError(f'Author source changed: rebuild/calibrate workspace ({path})')

def number(value):
    try:
        n = float(value)
    except (ValueError, TypeError):
        raise ImportError(f'Numeric keyframes only; bake expression {value!r} in Blockbench first') from None
    if not math.isfinite(n):
        raise ImportError('Non-finite keyframe')
    return n

def vector(value, default=0):
    if isinstance(value, dict):
        return np.array([number(value.get(axis, default)) for axis in ('x','y','z')])
    return np.array([number(v) for v in value])

def zyx(degrees):
    x,y,z = np.radians(degrees)
    return Rz(z) @ Ry(y) @ Rx(x)

def matrix(rotation, position):
    result = np.eye(4)
    result[:3,:3] = rotation
    result[:3,3] = position
    return result

def interpolate(keys, time, default):
    """Blockbench's Euler channels; preserve incoming/outgoing data points."""
    if not keys:
        return np.full(3, default, dtype=float)
    def point(key, outgoing=False):
        points = key['data_points']
        return vector(points[-1 if outgoing else 0], default)
    for k in keys:
        if abs(time-k['time']) < 1e-9:return point(k)
    if time < keys[0]['time']:
        return point(keys[0])
    if time >= keys[-1]['time']:
        return point(keys[-1], True)
    index = max(i for i,k in enumerate(keys) if k['time'] <= time)
    before, after = keys[index:index+2]
    p, q = point(before, True), point(after)
    gap = after['time'] - before['time']
    u = (time-before['time'])/gap
    a, b = before.get('interpolation','linear'), after.get('interpolation','linear')
    if a == 'step':
        return p
    if a == 'linear' and b in ('linear','step'):
        return p+(q-p)*u
    if a == 'catmullrom' or b == 'catmullrom':
        p0 = point(keys[index-1], True) if index else p
        p3 = point(keys[index+2]) if index+2 < len(keys) else q
        return .5*(2*p+(-p0+q)*u+(2*p0-5*p+4*q-p3)*u*u+(-p0+3*p-3*q+p3)*u*u*u)
    if a == 'bezier' or b == 'bezier':
        # Monotonic time handles, matching BB's clamped cubic control points.
        right = np.clip(vector(before.get('bezier_right_time',[.1]*3)),0,gap)/gap
        left = 1+np.clip(vector(after.get('bezier_left_time',[-.1]*3)),-gap,0)/gap
        p1 = p+vector(before.get('bezier_right_value',[0]*3))
        p2 = q+vector(after.get('bezier_left_value',[0]*3))
        lo, hi = np.zeros(3), np.ones(3)
        for _ in range(45):
            v=(lo+hi)/2
            x=3*(1-v)**2*v*right+3*(1-v)*v*v*left+v**3
            lo=np.where(x<u,v,lo);hi=np.where(x>=u,v,hi)
        v=(lo+hi)/2
        return (1-v)**3*p+3*(1-v)**2*v*p1+3*(1-v)*v*v*p2+v**3*q
    raise ImportError(f'Unsupported interpolation: {a}/{b}')

class Rig:
    def __init__(self, project, clip):
        self.project = project
        if project.get('meta',{}).get('model_format') != 'bedrock':
            raise ImportError('Keep this workspace in native Bedrock Animated Model format')
        self.meta = project.get('toi_workspace')
        if not self.meta:
            if project.get('model_identifier') == 'toi_weapon_workspace_v2':
                raise ImportError('Universal rig metadata missing; reopen with updated TOI helper before saving')
            # Blockbench without the helper drops custom root metadata on Save.
            self.meta = json.loads((HERE/'pale_workspace_rig.json').read_text())
        if self.meta.get('version') not in (1,2) or self.meta.get('euler_order') != 'ZYX':
            raise ImportError('Unsupported rig version / rotation order')
        migrated_counts = {}
        for path, expected in self.meta.get('source_sha256',{}).items():
            count = calibrated_source(path, expected, self.meta['version'])
            if count is not None:
                migrated_counts[path] = count
        live_a, live_model, _, _, live_grip = load(self.meta['archetype'])
        hand_names = [v+'_'+s+'hand' for v in ('firstperson','thirdperson') for s in ('right','left')]
        if (any(live_model['display'].get(k) != self.meta.get('display',{}).get(k) for k in hand_names) or
            live_a.get('firstperson_offset',[0,0,0]) != self.meta['fp_mount'] or
            live_a.get('firstperson_idle_offset',[0,0,0]) != self.meta['fp_idle_offset'] or
            live_grip is None or list(live_grip) != self.meta['grip']):
            raise ImportError('Renderer mounts/Display/grip changed; rebuild and recalibrate the rig')
        self.groups = {g['uuid']:g for g in project.get('groups',[])}
        self.names = {g['name']:g['uuid'] for g in self.groups.values()}
        if len(self.names) != len(self.groups):
            raise ImportError('Duplicate bone names')
        self.parents = {}
        def walk(entries, parent=None):
            for entry in entries:
                if isinstance(entry, dict):
                    uid = entry['uuid']
                    if uid not in self.groups:
                        raise ImportError(f'Missing bone record: {uid}')
                    if uid in self.parents:
                        raise ImportError('Duplicate/cyclic outliner entry')
                    self.parents[uid] = parent
                    walk(entry.get('children',[]),uid)
        walk(project['outliner'])
        if set(self.parents) != set(self.groups):raise ImportError('A bone is missing from the outliner')
        for view in ('FP','F5'):
            for side in ('right','left'):
                binding=self.meta['bindings'][view+'_'+side]
                for field in ('arm','shoulder','sword'):
                    if binding[field] not in self.names:
                        raise ImportError(f'Missing bone: {binding[field]}')
                if self.parents[self.names[binding['arm']]] != self.names[binding['shoulder']]:
                    raise ImportError('Arm must be a child of its shoulder')
                if self.parents[self.names[binding['sword']]] != self.names[binding['arm']]:
                    raise ImportError('Sword must be a child of its arm')
        # Edit-mode changes invalidate calibration; Animate keys remain free.
        if self.meta['version'] == 1:
            original_path=next(p for p in self.meta['source_sha256'] if p.endswith('.bbmodel'))
            originals=json.loads((ROOT/original_path).read_text())['elements']
            if original_path in migrated_counts:
                originals=originals[:migrated_counts[original_path]]
        else:
            originals=self.meta['source_cubes']
            self.check_live_geometry(originals,live_model)
        cubes={e['name']:e for e in project['elements'] if e.get('type','cube')=='cube'}
        for binding in self.meta['bindings'].values():
            for field, expected in (('shoulder',binding['pivot']),('arm',binding['pivot']),('sword',binding['grip_position'])):
                if not np.allclose(self.groups[self.names[binding[field]]]['origin'],expected,atol=1e-4,rtol=0):
                    raise ImportError('Edit-mode pivot changed; animate the calibrated bones instead')
            for i, original in enumerate(originals):
                cube=cubes.get(binding['sword']+'_cube_'+str(i))
                if cube is None:raise ImportError('Missing calibrated sword cube')
                if cube.get('faces') != original.get('atlas_faces',original['faces']):raise ImportError('Workspace sword UV changed')
                for field in ('from','to','origin'):
                    expected=np.array(binding['grip_position'])+binding['scale']*(np.array(original[field])-self.meta['grip'])
                    if not np.allclose(cube[field],expected,atol=1e-4,rtol=0):
                        raise ImportError('Editor sword geometry changed; restore the author copy')
                if not np.allclose(cube.get('rotation',[0,0,0]),original.get('rotation',[0,0,0]),atol=1e-4):
                    raise ImportError('Editor sword cube rotation changed')
        cameras={e['name'] for e in project['elements'] if e.get('type')=='camera'}
        if not set(self.meta['camera_names']) <= cameras:
            raise ImportError('Missing Cameras elements: install Cameras BEFORE opening/saving this project')
        matches=[a for a in project.get('animations',[]) if a['name']==clip]
        if len(matches)!=1:
            raise ImportError(f'Expected one clip {clip!r}; available: '+', '.join(a['name'] for a in project.get('animations',[])))
        self.clip=matches[0]
        if self.clip.get('loop','once') not in ('once','hold'):
            raise ImportError('Pilot accepts once/hold clips; bake cyclic wrap interpolation first')
        self.length=number(self.clip.get('length',0))
        if self.length<=0 or self.length>60 or number(self.clip.get('snapping',0))<=0:
            raise ImportError('Clip needs finite length (0..60 seconds) and positive snapping')
        if self.clip.get('anim_time_update') or self.clip.get('start_delay') or self.clip.get('loop_delay'):
            raise ImportError('Bake custom time expressions/delays first')
        self.channels={}
        self.times={0.,self.length}
        for uid, animator in self.clip.get('animators',{}).items():
            if uid not in self.groups:
                if animator.get('keyframes'):
                    raise ImportError('Only rig bone animation is supported (no camera/effects animation)')
                continue
            if animator.get('rotation_global') or animator.get('quaternion_interpolation'):
                raise ImportError('Bake global/quaternion rotation to local Euler keys first')
            channels={}
            for key in animator.get('keyframes',[]):
                channel=key['channel']
                if channel not in ('rotation','position','scale'):
                    raise ImportError(f'Unsupported bone channel: {channel}')
                time=number(key['time']);key=dict(key,time=time)
                if not 0<=time<=self.length or not key.get('data_points'):
                    raise ImportError('Key outside clip length / missing data points')
                if len(key['data_points'])>1 and key.get('interpolation')=='catmullrom':
                    raise ImportError('Bake split Catmull-Rom keys to ordinary keys first')
                for point in key['data_points']: vector(point,1 if channel=='scale' else 0)
                channels.setdefault(channel,[]).append(key)
                self.times.add(time)
                if key.get('interpolation')=='step' or len(key['data_points'])>1:
                    if time>0:self.times.add(max(0,time-.000001))
                    if len(key['data_points'])>1 and time<self.length:self.times.add(time+.000001)
            for keys in channels.values():
                keys.sort(key=lambda k:k['time'])
                for previous, following in zip(keys,keys[1:]):
                    if previous.get('interpolation')=='step':self.times.add(max(0,following['time']-.000001))
                if len({k['time'] for k in keys}) != len(keys):
                    raise ImportError('Duplicate key time in a channel')
            self.channels[uid]=channels
        # Blockbench Save includes empty animator containers for every rig bone.
        # Only bones with actual keyed channels count as animated; keep rejecting
        # non-empty body/head/camera tracks.
        animated_names={self.groups[uid]['name'] for uid, channels in self.channels.items() if any(channels.values())}
        allowed={b[k] for b in self.meta['bindings'].values() for k in ('arm','shoulder','sword')}
        if animated_names-allowed:
            raise ImportError('Animate shoulder, arm or sword bones only: '+', '.join(sorted(animated_names-allowed)))

    def check_live_geometry(self, originals, live_model):
        resolution=self.meta['source_resolution']
        def signature(c, editor):
            def rounded(v):return tuple(round(number(x),5) for x in v)
            if editor:
                r=c.get('rotation',[0,0,0]);origin=c['origin']
            else:
                rot=c.get('rotation',{})
                r=[rot.get('angle',0) if axis==rot.get('axis') else 0 for axis in 'xyz']
                if rot.get('rescale'):raise ImportError('Rescaled Java cube unsupported')
                origin=rot.get('origin',[0,0,0])
            faces=[]
            for name,face in sorted(c.get('faces',{}).items()):
                if editor and (face.get('texture') is None or face.get('texture') is False):continue
                uv=face.get('uv')
                if uv is None:raise ImportError('Java face needs explicit UV')
                if editor:uv=[v*16/resolution['width' if i%2==0 else 'height'] for i,v in enumerate(uv)]
                faces.append((name,rounded(uv),face.get('rotation',0)))
            return (rounded(c['from']),rounded(c['to']),rounded(r),rounded(origin) if any(r) else (0,0,0),tuple(faces))
        if sorted(signature(c,True) for c in originals) != sorted(signature(c,False) for c in live_model['elements']):
            raise ImportError('Editor weapon geometry/UV does not match installed Java model; export authored item first')

    def pose(self, time):
        result={}
        def bone(uid):
            if uid in result:return result[uid]
            g=self.groups[uid];parent=self.parents.get(uid)
            p=np.array(g['origin'],float)
            if parent:p-=np.array(self.groups[parent]['origin'],float)
            channels=self.channels.get(uid,{})
            delta=interpolate(channels.get('position',[]),time,0)
            rotation=vector(g.get('rotation',[0,0,0]))+interpolate(channels.get('rotation',[]),time,0)
            scale=interpolate(channels.get('scale',[]),time,1)
            if not np.allclose(scale,1,atol=1e-8):
                raise ImportError('Animated scale is unsupported; preserve author sword scale')
            local=matrix(zyx(rotation),p+delta)
            result[uid]=bone(parent)@local if parent else local
            return result[uid]
        return {name:bone(uid) for name,uid in self.names.items()}

def bake(project, clip, name='bb_test_guard', fps=60):
    if not name.startswith('bb_') or not name.replace('_','').isalnum():
        raise ImportError('New pilot names must start with bb_ and contain letters/digits/underscores')
    if not 20<=fps<=240:
        raise ImportError('Bake FPS must be 20..240')
    rig=Rig(project,clip)
    a, model, _, _, grip=load(rig.meta['archetype'])
    generated={}
    def empty():return {'length':rig.length,'hold':0,'return':0,'smooth':False,'rotation_mode':'slerp','third':{'baked':True},'keyframes':[]}
    for side in ('right','left'):
        suffix='' if side=='right' else '_off'
        n=name+suffix
        generated[n]=empty()
        generated[n]['third']={'source':n+'_shoulder','arm_space':True,'wrist_source':n+'_wrist','wrist_xyz':True,'baked':True}
        generated[n+'_shoulder']=empty();generated[n+'_wrist']=empty()
    times=sorted(rig.times | {min(rig.length,i/fps) for i in range(math.ceil(rig.length*fps)+1)})
    F=WORLD[:3,:3]
    def key(t,r,p=(0,0,0)):
        return {'t':round(t,8),'rotation':np.round(r,7).tolist(),'translation':np.round(p,7).tolist(),'ease':'linear'}
    for t in times:
        poses=rig.pose(t)
        for side,sign in (('right',1),('left',-1)):
            n=name+('' if sign==1 else '_off')
            binding=rig.meta['bindings']['FP_'+side]
            sword=poses[binding['sword']]
            display=model['display']['firstperson_'+side+'hand']
            rotation=euler_xyz(sword[:3,:3]@dR(display,sign).T)
            rotation[1:]*=sign
            mount=np.array(rig.meta['fp_mount'],float);mount[0]*=sign
            translation=(sword[:3,3]/16-np.array([sign*.56,-.52,-.72])-gp(grip,display,sign)-mount/16)*16
            translation[0]*=sign
            generated[n]['keyframes'].append(key(t,rotation,translation))
            binding=rig.meta['bindings']['F5_'+side]
            arm=poses[binding['arm']];sword=poses[binding['sword']]
            if not np.allclose(arm[:3,3],binding['pivot'],atol=1e-6):
                raise ImportError('F5 shoulder translation is unsupported; use rotation around the shoulder')
            local_grip=arm[:3,:3].T@(sword[:3,3]-arm[:3,3])
            expected=np.array(binding['grip_position'])-binding['pivot']
            if not np.allclose(local_grip,expected,atol=1e-6):
                raise ImportError('F5 sword position detaches the hilt; rotate sword at its grip instead')
            A=F.T@arm[:3,:3]@F@Rx(VANILLA_ARM[0])
            angles=euler_zyx(A);angles[0]-=math.degrees(VANILLA_ARM[0]);angles[1:]*=sign
            display=model['display']['thirdperson_'+side+'hand']
            wrist=(F@A@C).T@sword[:3,:3]@dR(display,sign).T
            generated[n+'_shoulder']['keyframes'].append(key(t,angles))
            generated[n+'_wrist']['keyframes'].append(key(t,euler_xyz(wrist)))
    if set(generated)&set(a['animations']):
        raise ImportError('Generated name collides with installed animation; choose a new --name')
    return {'animations':generated}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('project',type=Path)
    parser.add_argument('--clip',required=True)
    parser.add_argument('--name',default='bb_test_guard')
    parser.add_argument('--fps',type=int,default=60)
    parser.add_argument('--output',required=True,type=Path)
    parser.add_argument('--dry-run',action='store_true')
    args=parser.parse_args()
    try:
        if args.output.resolve().is_relative_to((ROOT/'src').resolve()):
            raise ImportError('Pilot export must stay outside src; review/game-test before integration')
        result=bake(json.loads(args.project.read_text()),args.clip,args.name,args.fps)
        if args.output.exists():
            old=json.loads(args.output.read_text())
            if set(old)!= {'animations'} or any(not n.startswith('bb_') for n in old['animations']):
                raise ImportError('Refusing to overwrite non-pilot output')
        text=json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+'\n'
        if args.dry_run:
            old=args.output.read_text().splitlines(True) if args.output.exists() else []
            sys.stdout.writelines(difflib.unified_diff(old,text.splitlines(True),fromfile=str(args.output),tofile=str(args.output)+' (preview)'))
        else:
            args.output.parent.mkdir(parents=True,exist_ok=True)
            args.output.write_text(text)
        print(f'{len(result["animations"])} new clips; {len(next(iter(result["animations"].values()))["keyframes"])} samples/clip; seconds, degrees, model pixels',file=sys.stderr)
    except (ImportError,ValueError,KeyError,OSError) as error:
        parser.exit(2,f'Import failed: {error}\n')
if __name__=='__main__':main()
