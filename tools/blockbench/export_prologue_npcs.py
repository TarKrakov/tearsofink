"""Export the box-UV Blockbench rigs and their embedded atlases as Fabric resources.

Run from any directory: python3 tools/blockbench/export_prologue_npcs.py [--check]
"""
import argparse
import base64
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'src/client/java/com/tarkrakov/tearsofink/client'
TEXTURES = ROOT / 'src/main/resources/assets/tears-of-ink/textures/entity'


def floats(values):
    return ', '.join(f'{(0 if abs(x) < 0.0000005 else x):.6f}'.rstrip('0').rstrip('.') + 'F'
                     for x in values)


def mesh_position(position):
    return [-position[0], -position[1], position[2]]


def offset(position, origin):
    return mesh_position([p - o for p, o in zip(position, origin)])


def generate(name, package, classname):
    model = json.loads((ROOT / 'blockbench' / f'{name}.bbmodel').read_text())
    if model['meta']['model_format'] != 'modded_entity' or not model['meta']['box_uv']:
        raise ValueError(f'{name} needs a Modded Entity project with Box UV')
    groups = {g['uuid']: g for g in model['groups']}
    elements = {e['uuid']: e for e in model['elements']}
    width, height = model['resolution']['width'], model['resolution']['height']
    lines = [f'package com.tarkrakov.tearsofink.client.{package};', '',
             'import net.minecraft.client.model.geom.PartPose;',
             'import net.minecraft.client.model.geom.builders.CubeDeformation;',
             'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
             'import net.minecraft.client.model.geom.builders.LayerDefinition;',
             'import net.minecraft.client.model.geom.builders.MeshDefinition;',
             'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
             f'/** Generated from blockbench/{name}.bbmodel. */',
             f'final class {classname} {{',
             '\tstatic LayerDefinition createBodyLayer() {',
             '\t\tMeshDefinition mesh = new MeshDefinition();',
             '\t\tPartDefinition root = mesh.getRoot();']
    visited = set()
    occupied = set()

    def emit(label, parent, position, parent_position, rotation, cubes, has_children):
        variable = f'part{len(lines)}' if has_children else None
        declaration = f'PartDefinition {variable} = ' if variable else ''
        lines.append(f'\t\t{declaration}{parent}.addOrReplaceChild("{label}", CubeListBuilder.create()')
        for e in cubes:
            u, v = e['uv_offset']
            dims = [round(b-a, 5) for a, b in zip(e['from'], e['to'])]
            if not all(int(d) == d and d > 0 for d in dims) or e.get('mirror_uv') or not e.get('box_uv'):
                raise ValueError(f'{name}: expected a positive, non-mirrored box UV cube: {e["name"]}')
            x, y, z = map(int, dims)
            if u < 0 or v < 0 or u + 2*(x+z) > width or v+y+z > height:
                raise ValueError(f'{name}: atlas range for {e["name"]}')
            expected = {'east':[u,v+z,u+z,v+z+y], 'north':[u+z,v+z,u+z+x,v+z+y],
                        'west':[u+z+x,v+z,u+2*z+x,v+z+y], 'south':[u+2*z+x,v+z,u+2*z+2*x,v+z+y],
                        'up':[u+z+x,v+z,u+z,v], 'down':[u+z+2*x,v,u+z+x,v+z]}
            if set(e['faces']) != set(expected):
                raise ValueError(f'{name}: incomplete faces for {e["name"]}')
            for face, coords in expected.items():
                f = e['faces'][face]
                if f['uv'] != coords or f['texture'] != 0 or f.get('rotation', 0):
                    raise ValueError(f'{name}: unsupported face UV for {e["name"]}/{face}')
                u1,v1,u2,v2=coords
                pixels = {(a,b) for a in range(min(u1,u2),max(u1,u2))
                                for b in range(min(v1,v2),max(v1,v2))}
                if occupied & pixels:
                    raise ValueError(f'{name}: overlapping UV for {e["name"]}/{face}')
                occupied.update(pixels)
            corners = [e['to'][0], e['to'][1], e['from'][2]]
            deform = f', new CubeDeformation({floats([e["inflate"]])})' if e.get('inflate') else ''
            args = floats(offset(corners, position) + dims)
            lines.append(f'\t\t\t\t.texOffs({u}, {v}).addBox({args}{deform})')
        triplet = offset(position, parent_position)
        if any(rotation):
            rotation = list(map(math.radians, mesh_position(rotation)))
            pose = f'PartPose.offsetAndRotation({floats(triplet + rotation)})'
        else:
            pose = f'PartPose.offset({floats(triplet)})' if any(triplet) else 'PartPose.ZERO'
        lines[-1] += f', {pose});'
        return variable

    def group(node, parent, parent_position):
        g = groups[node['uuid']]
        if not g.get('export', True):
            return
        local_cubes = [elements[c] for c in node['children'] if isinstance(c,str) and elements[c].get('export', True)]
        rotated = [c for c in local_cubes if any(c.get('rotation', []))]
        nested = [n for n in node['children'] if isinstance(n,dict)]
        var = emit(g['name'], parent, g['origin'], parent_position, g['rotation'],
                   [c for c in local_cubes if c not in rotated], bool(rotated or nested))
        for e in local_cubes:
            if e['uuid'] in visited:
                raise ValueError(f'{name}: repeated cube {e["name"]}')
            visited.add(e['uuid'])
        for i, e in enumerate(rotated):
            emit(f'cube_{i+1}', var, e['origin'], g['origin'], e['rotation'], [e], False)
        for n in nested:
            group(n, var, g['origin'])

    for node in model['outliner']:
        group(node, 'root', (0,24,0))
    if visited != {key for key, e in elements.items() if e.get('export', True)}:
        raise ValueError(f'{name}: unreferenced element')
    lines += [f'\t\treturn LayerDefinition.create(mesh, {width}, {height});', '\t}', '}', '']
    yield JAVA / package / f'{classname}.java', '\n'.join(lines).encode()
    for atlas, suffix in zip(model['textures'], ('', '_eyes'), strict=True):
        raw = base64.b64decode(atlas['source'].removeprefix('data:image/png;base64,'), validate=True)
        w = int.from_bytes(raw[16:20], 'big')
        h = int.from_bytes(raw[20:24], 'big')
        if not raw.startswith(b'\x89PNG\r\n\x1a\n') or (w, h) != (2*width, 2*height):
            raise ValueError(f'{name}: unexpected atlas data or size')
        yield TEXTURES / f'{name}{suffix}.png', raw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='check committed Java and PNG files')
    args = parser.parse_args()
    for spec in [('guide','prologue','GuideGeometry'), ('soul_trader','trader','SoulTraderGeometry')]:
        for path, data in generate(*spec):
            if args.check:
                if not path.is_file() or path.read_bytes() != data:
                    parser.exit(1, f'Regenerate {path.relative_to(ROOT)}\n')
            else:
                path.write_bytes(data)


if __name__ == '__main__':
    main()
