// Run: node tools/blockbench/test_workspace.js
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const build=require('./toi_workspace.js');
const template=JSON.parse(fs.readFileSync(path.join(__dirname,'pale_animation_workspace.bbmodel'),'utf8'));
const source=JSON.parse(fs.readFileSync(path.join(__dirname,'../../src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel'),'utf8'));
const m=template.toi_workspace;
let id=0;
const cfg={archetype:m.archetype,grip:m.grip,display:m.display,fp_mount:m.fp_mount,fp_idle_offset:m.fp_idle_offset,makeId:()=>String(++id)};
const ids=[...new Set(source.elements.flatMap(e=>Object.values(e.faces).filter(f=>f.texture!==null&&f.texture!==false).map(f=>f.texture)))];
const atlas={width:64,height:32,itemWidth:32,itemHeight:32,textureIds:ids,source:'data:image/png;base64,test'};
const before=JSON.stringify(template),sourceBefore=JSON.stringify(source);
const result=build(template,source,cfg,atlas);
for(const b of Object.keys(m.bindings))for(const k of ['pivot','grip_position','item_rotation']){
 const a=m.bindings[b][k].flat(),z=result.toi_workspace.bindings[b][k].flat();
 assert(a.every((v,i)=>Math.abs(v-z[i])<1e-7),b+' '+k);
}
assert.equal(result.elements.filter(e=>/_sword_cube_\d+$/.test(e.name)).length,124);
assert.equal(result.animations.length,0);
assert.equal(build(result,source,cfg,atlas).elements.length,result.elements.length);
assert.equal(JSON.stringify(template),before);assert.equal(JSON.stringify(source),sourceBefore);
assert.throws(()=>build(template,source,{...cfg,grip:[0,NaN,0]},atlas));
console.log('TOI universal workspace geometry checks passed');


// Automatic F5: both real authored template clips, no mutations, both hands.
const {planF5,planTiming,rot,euler}=build;
const clone=x=>JSON.parse(JSON.stringify(x));
const gByName=n=>template.groups.find(g=>g.name===n);
const f5Ids=new Set(template.groups.filter(g=>g.name.startsWith('F5_')).map(g=>g.uuid));
const clearF5=a=>{a=clone(a);for(const id of f5Ids)delete a.animators[id];return a};
const single=clearF5(template.animations[0]),dual=clearF5(template.animations[1]);
const old=JSON.stringify(template),oldSingle=JSON.stringify(single);
const one=planF5(template,single),two=planF5(template,dual);
assert.equal(one.status,'generated');assert.deepEqual(one.sides,['right']);
assert.equal(two.status,'generated');assert.deepEqual(two.sides,['right','left']);
assert.equal(Object.keys(one.animators).length,3);assert.equal(Object.keys(two.animators).length,6);
assert.equal(planF5(template,template.animations[0]).status,'protected');
assert.equal(planF5(template,template.animations[0],true).status,'generated');
assert.equal(JSON.stringify(template),old);assert.equal(JSON.stringify(single),oldSingle);
assert.equal(planF5(template,{...single,animators:{}}).status,'empty');
const mm=(a,b)=>a.map(r=>b[0].map((_,j)=>r.reduce((s,x,k)=>s+x*b[k][j],0)));
const assertMatrix=(a,b)=>assert(a.flat().every((x,i)=>Math.abs(x-b.flat()[i])<1e-5));
const vec=k=>['x','y','z'].map(a=>Number(k?.data_points[0][a]||0));
const add=(a,b)=>a.map((x,i)=>x+b[i]);
const readPose=(clip,name,t)=>{const g=gByName(name),k=clip.animators[g.uuid]?.keyframes?.find(k=>k.time===t&&k.channel==='rotation');return rot(add(g.rotation,vec(k)))};
// At the original keys: FP blade orientation is preserved in editor space,
// F5 arm rotation is around the calibrated pivot; no position/scale keys.
for(const side of ['right','left'])for(const t of [0,.25,.75,1]){
  const arm=gByName('F5_'+side+'_arm'),sword=gByName('F5_'+side+'_sword');
  const fp=mm(mm(readPose(dual,'FP_'+side+'_shoulder',t),readPose(dual,'FP_'+side+'_arm',t)),readPose(dual,'FP_'+side+'_sword',t));
  const generated=g=>rot(add(g.rotation,vec(two.animators[g.uuid].keyframes.find(k=>k.time===t))));
  assertMatrix(mm(generated(arm),generated(sword)),fp);
}
for(const a of Object.values(two.animators))for(const k of a.keyframes){assert.equal(k.channel,'rotation');assert(vec(k).every(Number.isFinite));}
const g= gByName('FP_right_arm');
const malformed=clone(single);malformed.animators[g.uuid].keyframes[0].data_points[0].x='query.life_time';
assert.throws(()=>planF5(template,malformed),/Molang/);
const shifted=clone(template);shifted.groups.find(x=>x.uuid===g.uuid).origin[0]+=1;assert.throws(()=>planF5(shifted,single),/pivot/);
const manual=clone(template.animations[0]);const armId=gByName('F5_right_arm').uuid;
manual.animators[armId].keyframes=manual.animators[armId].keyframes.filter(k=>[0,1].includes(k.time));
const sync=planTiming(template,manual);
assert(sync.inserted>=2);assert(sync.animators[armId].keyframes.some(k=>k.time===.25));
assert.deepEqual(sync.animators[armId].keyframes.slice(0,2),manual.animators[armId].keyframes);
manual.animators[armId].keyframes[0].interpolation='catmullrom';
assert(!planTiming(template,manual).animators[armId]);
for(const a of [[22,35,-80],[0,90,30],[75,-90,20],[179,20,-175]]){
 const R=rot(a);assertMatrix(rot(euler(R,a)),R);
}
// Universal v2 constructed from the real source has independently calibrated geometry.
const universal=clone(result);universal.animations=[single];assert.equal(planF5(universal,single).status,'generated');
console.log('TOI automatic F5 geometry, both hands, protection, validation and timing checks passed');


// Recovery from an existing artist scene after hot reload or metadata loss.
const {recoverMetadata}=build;
const missing=clone(template);delete missing.toi_workspace;missing.name='pale_linear_greatsword_animations';
missing.animations=[{...single,name:'attack1'},{...dual,name:'dual_attack1'}];
const beforeRecovery=JSON.stringify(missing);
const recovered=recoverMetadata(missing);
assert.deepEqual(recovered,template.toi_workspace);
assert.equal(JSON.stringify(missing),beforeRecovery);
assert.equal(planF5({...missing,toi_workspace:recovered},missing.animations[0]).status,'generated');
const v2Missing=clone(result);delete v2Missing.toi_workspace;
assert.deepEqual(recoverMetadata(v2Missing,result),result.toi_workspace);
const wrong=clone(missing);wrong.elements.find(e=>e.name==='FP_right_sword_cube_0').from[0]+=1;
assert.equal(recoverMetadata(wrong),null);
const wrongPivot=clone(missing);wrongPivot.groups.find(g=>g.name==='FP_right_arm').origin[0]+=1;
assert.equal(recoverMetadata(wrongPivot),null);
const wrongDisk=clone(result);wrongDisk.elements.find(e=>e.name==='FP_right_sword_cube_0').to[0]+=1;
assert.equal(recoverMetadata(wrong,wrongDisk),null);
console.log('TOI calibration recovery: metadata-free authored clips, disk v2, reject altered geometry and pivots passed');
