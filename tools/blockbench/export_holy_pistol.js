// Exact export of the author's original pistol. Run from any directory with Node.
// No author source is overwritten; validate both derived files with --check.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {normalizePistolSource,pistolLeft}=require('./toi_workspace.js');
function convert(source){
 const j=normalizePistolSource(source);
 const model={credit:"Author's holy_pistol.bbmodel; quarter rotations baked without geometry/UV changes",textures:{"0":"tears-of-ink:item/holy_pistol_texture",particle:"tears-of-ink:item/holy_pistol_texture"},elements:j.elements.map(e=>{
   const out={name:e.name,from:e.from,to:e.to};
   const a=(e.rotation||[]).findIndex(n=>n!==0);
   if(a>=0){const angle=e.rotation[a];out.rotation={angle,axis:"xyz"[a],origin:e.origin};if(![0,22.5,-22.5,45,-45].includes(angle))out.rotation.toi_exact_pistol=true;}
   out.faces=Object.fromEntries(Object.entries(e.faces).map(([k,v])=>[k,{uv:v.uv.map((n,i)=>n*16/(i%2?j.resolution.height:j.resolution.width)),texture:"#0",...(v.rotation?{rotation:v.rotation}:{})}]));
   return out;
 }),display:j.display};
 return {source:j,model};
}
const root=path.resolve(__dirname,'../..'),assets=path.join(root,'src/main/resources/assets/tears-of-ink');
const original=JSON.parse(fs.readFileSync(path.join(root,'blockbench/holy_pistol.bbmodel'),'utf8'));
const r=convert(original),outputs=[[path.join(assets,'models/item/holy_pistol.bbmodel'),r.source],[path.join(assets,'models/item/holy_pistol.json'),r.model]];
for(const [filename,data] of outputs){
 if(process.argv.includes('--check'))assert.deepEqual(JSON.parse(fs.readFileSync(filename,'utf8')),data);
 else fs.writeFileSync(filename,JSON.stringify(data,null,2)+'\n');
}
assert.equal(original.elements.length,r.model.elements.length);
assert.deepEqual(r.model.display.firstperson_righthand,original.display.firstperson_righthand);
assert.deepEqual(r.model.display.thirdperson_righthand,original.display.thirdperson_righthand);
const texture=fs.readFileSync(path.join(assets,'textures/item/holy_pistol_texture.png'));
assert(texture.equals(Buffer.from(original.textures[0].source.split(',')[1],'base64')));
console.log('Pistol: '+r.model.elements.length+' cubes, exact authored UV/texture/right display, mirrored left display');

if(process.argv.includes('--check')){
 const build=require('./toi_workspace.js');
 const template=JSON.parse(fs.readFileSync(path.join(__dirname,'pale_animation_workspace.bbmodel'),'utf8'));
 let uid=0;
 const cfg={archetype:'holy_pistol',grip:[10,6.5,11],display:r.source.display,fp_mount:[0,0,0],fp_idle_offset:[0,0,0],makeId:()=>`pistol-test-${++uid}`};
 const atlas={width:96,height:64,itemWidth:64,itemHeight:64,textureIds:[0],source:'data:image/png;base64,test'};
 assert.deepEqual(normalizePistolSource(original),r.source);
 assert.deepEqual(normalizePistolSource(r.source),r.source);
 const originalSnapshot=JSON.stringify(original);
 const scene=build(template,original,{...cfg,display:normalizePistolSource(original).display},atlas);
 assert.equal(JSON.stringify(original),originalSnapshot);
 assert.deepEqual(scene.toi_workspace.source_cubes,build(template,r.source,cfg,atlas).toi_workspace.source_cubes);
 assert.equal(scene.elements.filter(e=>/_sword_cube_\d+$/.test(e.name)).length,56);
 // Check physical mirror, not just matching JSON. World reflection X and
 // local reflection Z around the true grip must match both muzzle and grip.
 const reflect=v=>[-v[0],v[1],v[2]];
 const near=(a,b)=>a.every((v,i)=>Math.abs(v-b[i])<1e-5);
 const add=(a,b)=>a.map((v,i)=>v+b[i]);
 const mul=(m,v)=>m.map(row=>row.reduce((s,x,i)=>s+x*v[i],0));
 const grip=cfg.grip;
 for(const view of ['FP','F5']){
  const right=scene.toi_workspace.bindings[view+'_right'],left=scene.toi_workspace.bindings[view+'_left'];
  assert(near(left.grip_position,reflect(right.grip_position)),view+' grip mirror');
  for(const v of [[1,0,0],[0,1,0],[0,0,1],[12.5,3.5,0]]){
   const local=[v[0],v[1],-v[2]];
   assert(near(mul(left.item_rotation,local),reflect(mul(right.item_rotation,v))),view+' muzzle/up/side mirror');
  }
 }
 const explicit=JSON.parse(JSON.stringify(original));
 explicit.display.firstperson_lefthand={rotation:[1,2,3],translation:[4,5,6]};
 assert.deepEqual(normalizePistolSource(explicit).display.firstperson_lefthand,explicit.display.firstperson_lefthand);
 const ids=new Set(template.groups.filter(g=>g.name.startsWith('FP_')).map(g=>g.uuid));
 const clip=(i,name)=>({...template.animations[i],name,animators:Object.fromEntries(Object.entries(template.animations[i].animators).filter(([id])=>ids.has(id)))});
 scene.animations=[clip(0,'aim_enter'),clip(1,'dual_shoot')];
 // Pistol's asymmetric rest angles need matrix reflection, including wrist.
 const originalSingle=scene.animations[0],beforeMirror=JSON.stringify(originalSingle);
 const mirrored=build.planMirrorRight(scene,originalSingle);
 const paired={...originalSingle,animators:{...originalSingle.animators,...mirrored.animators}};
 assert.equal(JSON.stringify(originalSingle),beforeMirror);
 assert.deepEqual(mirrored.views,['FP']);
 assert.deepEqual(build.planF5(scene,paired).sides,['right','left']);
 assert(build.planMirrorRight(scene,paired).replaced>0);
 const synthetic={name:'mirror_test',length:1,loop:'once',snapping:30,animators:{}};
 const rg=scene.groups.find(g=>g.name==='FP_right_sword'),lg=scene.groups.find(g=>g.name==='FP_left_sword');
 const k=(time,channel,v)=>({time,channel,interpolation:'linear',data_points:[{x:String(v[0]),y:String(v[1]),z:String(v[2])}]});
 synthetic.animators[rg.uuid]={name:rg.name,type:'bone',keyframes:[k(0,'rotation',[0,0,0]),k(1,'rotation',[31,-44,67]),k(0,'position',[0,0,0]),k(1,'position',[2,3,4])]};
 const reflected=build.planMirrorRight(scene,synthetic).animators[lg.uuid].keyframes;
 const mulM=(a,b)=>a.map(row=>b[0].map((_,j)=>row.reduce((s,v,i)=>s+v*b[i][j],0)));
 const transpose=a=>a[0].map((_,i)=>a.map(row=>row[i]));
 const M=[[-1,0,0],[0,1,0],[0,0,1]],A=build.rot(rg.rotation),B=build.rot(lg.rotation);
 const value=k=>['x','y','z'].map(n=>Number(k.data_points[0][n]));
 for(const t of [0,.25,.5,.75,1]){
  const actual=reflected.find(k=>k.channel==='rotation'&&Math.abs(k.time-t)<1e-7);
  assert(actual);
  const source=build.rot(add(rg.rotation,[31*t,-44*t,67*t]));
  const expected=mulM(mulM(mulM(mulM(M,source),transpose(A)),M),B);
  const got=build.rot(add(lg.rotation,value(actual)));
  assert(got.every((row,i)=>near(row,expected[i])),'Pistol mirror at '+t);
 }
 assert.deepEqual(value(reflected.find(k=>k.channel==='position'&&k.time===1)),[-2,3,4]);
 const pairedBake=build.bakeGame(scene,paired,'bb_author_dual_aim_enter',60);
 assert(pairedBake.animations.bb_author_dual_aim_enter_off);
 console.log('Pistol mirror: calibrated wrist matrix, position, both hands and game bake passed');
 for(const a of scene.animations){
  const baked=build.bakeGame(scene,a,'bb_author_'+a.name,60);
  assert(baked.maxGripError<1e-5);assert(baked.maxRotationError<1e-5);
  assert(baked.animations['bb_author_'+a.name]);
  if(a.name.startsWith('dual_'))assert(baked.animations['bb_author_'+a.name+'_off']);
 }
 const tmp=fs.mkdtempSync(path.join(require('node:os').tmpdir(),'toi-pistol-'));
 try{
  const file=path.join(tmp,'holy_pistol_animation.bbmodel');fs.writeFileSync(file,JSON.stringify(scene));
  require('node:child_process').execFileSync(process.execPath,[path.join(__dirname,'import_holy_pistol.js'),file],{stdio:'inherit'});
 }finally{fs.rmSync(tmp,{recursive:true,force:true});}
 console.log('Pistol workspace: FP/F5 single and dual dry-run bake succeeded');
}
