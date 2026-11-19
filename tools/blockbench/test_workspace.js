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
