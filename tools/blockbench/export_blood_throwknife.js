// Reproducible export from the untouched author source; --check is read-only.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const build=require('./toi_workspace.js'),{normalizeThrowknifeSource}=build;
function convert(source){
 const j=normalizeThrowknifeSource(source);
 const model={credit:"Author's blood_throwknife.bbmodel, unchanged geometry and UV",textures:{"0":"tears-of-ink:item/blood_throwknife_texture",particle:"tears-of-ink:item/blood_throwknife_texture"},
 elements:j.elements.filter(e=>e.export!==false).map(e=>{
   if((e.rotation||[]).some(n=>n!==0))throw Error("Unexpected rotated cube");
   return {name:e.name,from:e.from,to:e.to,faces:Object.fromEntries(Object.entries(e.faces).map(([k,v])=>[k,{uv:v.uv.map((n,i)=>n*16/(i%2?j.resolution.height:j.resolution.width)),texture:"#0",...(v.rotation?{rotation:v.rotation}:{})}]))};
 }),
 display:{...j.display,gui:{rotation:[0,90,0],translation:[1,1.5,-1],scale:[1,1,1]},ground:{rotation:[0,90,0],translation:[.7,1.05,-.7],scale:[.7,.7,.7]}}};
 return {source:j,model};
}
const root=path.resolve(__dirname,'../..'),assets=path.join(root,'src/main/resources/assets/tears-of-ink');
const original=JSON.parse(fs.readFileSync(path.join(root,'blockbench/blood_throwknife.bbmodel'),'utf8'));
const r=convert(original);
const outputs=[[path.join(assets,'models/item/blood_throwing_dagger.bbmodel'),r.source],[path.join(assets,'models/item/blood_throwing_dagger.json'),r.model]];
const check=process.argv.includes('--check');
for(const [filename,data] of outputs){
 if(check)assert.deepEqual(JSON.parse(fs.readFileSync(filename,'utf8')),data);
 else fs.writeFileSync(filename,JSON.stringify(data,null,2)+'\n');
}
const texture=Buffer.from(original.textures[0].source.split(',')[1],'base64'),png=path.join(assets,'textures/item/blood_throwknife_texture.png');
if(check)assert(fs.readFileSync(png).equals(texture));else fs.writeFileSync(png,texture);
assert.deepEqual(r.source.elements,original.elements);
assert.deepEqual(r.source.textures,original.textures);
assert.deepEqual(normalizeThrowknifeSource(r.source),r.source);
for(const view of ['firstperson','thirdperson'])assert.deepEqual(r.model.display[view+'_righthand'],original.display[view+'_righthand']);
const template=JSON.parse(fs.readFileSync(path.join(__dirname,'pale_animation_workspace.bbmodel'),'utf8'));
let uid=0;
const cfg={archetype:'blood_throwing_dagger',grip:[7,2,7],display:r.source.display,fp_mount:[0,0,0],fp_idle_offset:[0,0,0],makeId:()=>`throwknife-test-${++uid}`};
const atlas={width:64,height:32,itemWidth:32,itemHeight:32,textureIds:[0],source:'data:image/png;base64,test'};
const scene=build(template,original,cfg,atlas);
assert.equal(scene.elements.filter(e=>/_sword_cube_\d+$/.test(e.name)).length,20);
const near=(a,b)=>a.every((v,i)=>Math.abs(v-b[i])<1e-5),reflect=v=>[-v[0],v[1],v[2]],mul=(m,v)=>m.map(row=>row.reduce((s,x,i)=>s+x*v[i],0));
for(const view of ['FP','F5']){
 const right=scene.toi_workspace.bindings[view+'_right'],left=scene.toi_workspace.bindings[view+'_left'];
 assert(near(left.grip_position,reflect(right.grip_position)),view+' grip reflection');
 for(const v of [[0,12,0],[0,0,1],[1,0,0]])assert(near(mul(left.item_rotation,reflect(v)),reflect(mul(right.item_rotation,v))),view+' blade reflection');
}
console.log('Throwknife: five authored cubes, exact texture/UV, both hands calibrated.');
