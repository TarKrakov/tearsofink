// Verify final author scene, the calibrated exporter and actual installed resources.
// Run: node tools/blockbench/check_final_pale.js
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {bakeGame,compatibleMetadata}=require('./toi_workspace.js');
const root=path.resolve(__dirname,'../..'),read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const scene=read('blockbench/pale_linear_greatsword_animation.bbmodel');
const installed=read('src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword_bb_author.json');
const combo=read('src/main/resources/data/tears-of-ink/weapon_combos/pale_linear_greatsword.json');
assert(compatibleMetadata(scene,scene.toi_workspace));
assert.equal(Object.keys(installed.source_map).length,18);
assert.equal(scene.animations.length,21);
const live=installed.animations,expectedNames=new Set();
let calculated=0,totalSamples=0,maxError=0,maxGripError=0;
for(const [name,prefix] of Object.entries(installed.source_map)){
  const source=scene.animations.find(a=>a.name===name);assert(source,'missing source '+name);
  const baked=bakeGame(scene,source,prefix,60);
  if(baked.autoF5)calculated++;
  totalSamples+=baked.samples;maxError=Math.max(maxError,baked.maxRotationError);maxGripError=Math.max(maxGripError,baked.maxGripError);
  const paired=name.startsWith('dual_');
  const scale=installed.time_scale_from_source[name]||1;
  for(const [n,c] of Object.entries(baked.animations)){
    expectedNames.add(n);assert(live[n],n);
    if(paired||n.endsWith('_shoulder')||n.endsWith('_wrist')){
      const expected=paired?c.keyframes:c.keyframes.map(k=>({...k,t:Number((k.t*scale).toFixed(8))}));
      assert.deepEqual(live[n].keyframes,expected,n);
      assert(Math.abs(live[n].length-c.length*(paired?1:scale))<1e-8,n);
    }
    if(n===prefix||n===prefix+'_off'){
      assert.equal(live[n].third.source,n+'_shoulder');assert.equal(live[n].third.wrist_source,n+'_wrist');
      assert(live[n].third.arm_space&&live[n].third.wrist_xyz,n);
    }
    for(const k of live[n].keyframes)assert([...k.rotation,...k.translation,k.t].every(Number.isFinite),n);
  }
}
assert.deepEqual([...expectedNames].sort(),Object.keys(live).sort());
assert.equal(calculated,6);assert(maxGripError<1e-5&&maxError<1e-5);
assert.deepEqual(combo.dual_steps.map(s=>s.anim),[1,2,2,2,3].map(n=>'bb_author_dual_attack'+n));
for(const step of combo.dual_steps){assert.equal(step.off_anim,step.anim+'_off');assert(live[step.anim]&&live[step.off_anim]);assert(step.hit<=step.duration);assert.equal(live[step.anim].impact,step.hit/20);}
for(const stem of ['start','hold'])for(const side of ['','_off']){
  const c=live['bb_author_dual_guard_'+stem+side];assert(c.length+c.hold+c.return>1.5,'guard refresh');
}
console.log('PASS: 18 authored clips; '+calculated+' F5 pairs generated, '+totalSamples+' samples; max matrix error '+maxError+', hilt error '+maxGripError);
