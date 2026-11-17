"""Run: python -m unittest discover -s tools/blockbench -p 'test_*.py'."""
import base64
import copy
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
import numpy as np
from PIL import Image
from import_weapon_animation import HERE, ROOT, Rig, bake, ImportError, interpolate, zyx
from third import WORLD, VANILLA_ARM, Rx, Rz, Ry, XYZ, gp, dR, sample, fp_item_m, fp_offset, part_m, tp_item_m, axis
from run import load
from tl import hand_pose

PROJECT=HERE/'pale_animation_workspace.bbmodel'
DEMO='animation.toi.demo_dual_guard'

class PipelineTest(unittest.TestCase):
    def setUp(self):
        self.project=json.loads(PROJECT.read_text())
        self.a,self.model,_,_,self.grip=load('pale_linear_greatsword')

    def assert_roundtrip(self, project, clip):
        rig=Rig(project,clip);clips=bake(project,clip)['animations']
        tune=dict(reach=1.3,lead=.5,body=.3,look=.6)
        # Every bake sample plus intermediate times measure the actual linear/slerp approximation.
        for t in np.linspace(0,1,121):
            poses=rig.pose(float(t))
            for side,sign in (('right',1),('left',-1)):
                n='bb_test_guard'+('' if sign==1 else '_off');c=clips[n]
                bind=rig.meta['bindings']['FP_'+side];bb=poses[bind['sword']]
                expected=np.eye(4);expected[:3,:3]=bb[:3,:3]*bind['scale']
                expected[:3,3]=bb[:3,3]/16-expected[:3,:3]@np.array(self.grip)/16
                d=self.model['display']['firstperson_'+side+'hand']
                actual=fp_item_m(d,*sample(c,t),self.grip,sign,view_offset=fp_offset(self.model))
                np.testing.assert_allclose(actual,expected,atol=1e-7 if abs(t*60-round(t*60))<1e-6 else 3e-4)
                result=hand_pose('pale_linear_greatsword',self.model,self.grip,c,t,sign,tune,animations=clips)
                arm,wrist,tp,handle=result[:4]
                actual=tp_item_m(WORLD@part_m([-sign*5,2,0],*arm),tp,handle,wrist,sign)
                bind=rig.meta['bindings']['F5_'+side];bb=poses[bind['sword']]
                expected[:3,:3]=bb[:3,:3]*bind['scale']
                expected[:3,3]=bb[:3,3]/16-expected[:3,:3]@np.array(self.grip)/16
                np.testing.assert_allclose(actual,expected,atol=1e-7 if abs(t*60-round(t*60))<1e-6 else 3e-4)
                np.testing.assert_allclose((actual@np.r_[np.array(self.grip)/16,1])[:3],bb[:3,3]/16,atol=1e-7 if abs(t*60-round(t*60))<1e-6 else 2e-4)
        return clips

    def test_both_demos_hierarchy_and_grip(self):
        for clip in ('animation.toi.demo_single_guard',DEMO):
            self.assert_roundtrip(self.project,clip)

    def test_independent_wrist_parent_and_fp_translation(self):
        p=copy.deepcopy(self.project);anim=p['animations'][1]
        for uid,a in anim['animators'].items():
            if a['name']=='FP_right_arm':
                a['keyframes'].append({'time':.5,'channel':'position','interpolation':'linear','data_points':[{'x':'1','y':'2','z':'-1'}]})
            if a['name']=='F5_left_sword':
                for k in a['keyframes']:
                    k['data_points'][0]={'x':'17','y':'-9','z':'23'} if k['time'] in (.25,.75) else {'x':'0','y':'0','z':'0'}
        self.assert_roundtrip(p,DEMO)

    def test_neutral_visual_pose_and_determinism(self):
        clips=bake(self.project,DEMO)
        self.assertEqual(clips,bake(self.project,DEMO))
        for n,c in clips['animations'].items():
            if n.endswith(('_shoulder','_wrist')):
                np.testing.assert_allclose(c['keyframes'][0]['rotation'],[0,0,0],atol=1e-6)
            else:
                np.testing.assert_allclose(c['keyframes'][0]['translation'],[0,0,-.64],atol=1e-6)
        self.assert_roundtrip(self.project,DEMO)

    def test_opaque_guides_and_author_sword_fidelity(self):
        author=json.loads((ROOT/'src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel').read_text())
        atlas=Image.open(io.BytesIO(base64.b64decode(self.project['textures'][0]['source'].split(',')[1])))
        original=Image.open(io.BytesIO(base64.b64decode(author['textures'][0]['source'].split(',')[1])))
        self.assertEqual(atlas.crop((0,0,32,32)).tobytes(),original.convert('RGBA').tobytes())
        cubes=[e for e in self.project['elements'] if e.get('type')=='cube']
        swords=[e for e in cubes if '_sword_cube_' in e['name']]
        self.assertEqual(len(swords),124)
        rig=Rig(self.project,DEMO)
        for binding in rig.meta['bindings'].values():
            for i,src in enumerate(author['elements']):
                e=next(e for e in swords if e['name']==binding['sword']+'_cube_'+str(i))
                for field in ('from','to','origin'):
                    recovered=(np.array(e[field])-binding['grip_position'])/binding['scale']+self.grip
                    np.testing.assert_allclose(recovered,src[field],atol=1e-7)
                self.assertEqual(e['faces'],src['faces'])
        for e in cubes:
            if e in swords:continue
            for f in e['faces'].values():
                self.assertEqual(f['texture'],0)
                u,v=f['uv'][:2]
                self.assertEqual(atlas.getpixel((int(u*2),int(v*2)))[3],255)
        for e in self.project['elements']+self.project['groups']:
            if e['name'].startswith('FP_'):self.assertFalse(e['visibility'])

    def test_reject_detached_grip_scale_missing_camera_expression(self):
        for failure in ('position','scale','camera','expression'):
            p=copy.deepcopy(self.project)
            if failure=='camera':p['elements']=[e for e in p['elements'] if e.get('type')!='camera']
            else:
                a=next(a for a in p['animations'][1]['animators'].values() if a['name']=='F5_right_sword')
                if failure=='expression':a['keyframes'][1]['data_points'][0]['x']='query.anim_time'
                else:a['keyframes'].append({'time':.5,'channel':failure,'interpolation':'linear','data_points':[{'x':'2','y':'1','z':'1'}]})
            with self.assertRaises(ImportError):bake(p,DEMO)

    def test_interpolation_and_dry_run(self):
        keys=[{'time':0,'interpolation':'step','data_points':[{'x':1,'y':2,'z':3}]},{'time':1,'interpolation':'linear','data_points':[{'x':9,'y':10,'z':11}]}]
        np.testing.assert_equal(interpolate(keys,.9,0),[1,2,3])
        np.testing.assert_equal(interpolate(keys,1,0),[9,10,11])
        for kind in ('linear','catmullrom','bezier'):
            keys[0]['interpolation']=kind;keys[1]['interpolation']=kind
            np.testing.assert_allclose(interpolate(keys,.5,0),[5,6,7],atol=1e-8)
        keys[0]['interpolation']='catmullrom';keys[1]['interpolation']='catmullrom'
        np.testing.assert_allclose(interpolate(keys,.25,0),[2.625,3.625,4.625])
        keys[0]['interpolation']='bezier';keys[1]['interpolation']='bezier'
        keys[0]['bezier_right_time']=[1/3]*3;keys[1]['bezier_left_time']=[-1/3]*3
        np.testing.assert_allclose(interpolate(keys,.25,0),[2.25,3.25,4.25],atol=1e-8)
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/'out.json'
            cmd=[sys.executable,str(HERE/'import_weapon_animation.py'),str(PROJECT),'--clip',DEMO,'--output',str(out)]
            r=subprocess.run(cmd+['--dry-run'],capture_output=True,text=True)
            self.assertEqual(r.returncode,0,r.stderr);self.assertFalse(out.exists());self.assertIn('+',r.stdout)
            subprocess.run(cmd,check=True,capture_output=True)
            old=out.read_bytes();r=subprocess.run(cmd+['--dry-run'],capture_output=True,text=True)
            self.assertEqual(r.returncode,0,r.stderr);self.assertEqual(r.stdout,'');self.assertEqual(old,out.read_bytes())

if __name__=='__main__':unittest.main()
