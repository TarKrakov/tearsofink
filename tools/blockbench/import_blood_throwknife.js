// Import the author's Save for game scene without changing its keys or art.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const build=require('./toi_workspace.js');
function importThrowknife(p,canonical,base,model){
 const m=p.toi_workspace;
 assert.equal(m?.archetype,'blood_throwing_dagger');assert.deepEqual(m.grip,base.grip);
 assert.deepEqual(m.fp_mount,base.firstperson_offset);assert.deepEqual(m.fp_idle_offset,base.firstperson_idle_offset);
 const near=(a,b)=>Array.isArray(a)&&Array.isArray(b)&&a.length===b.length&&a.every((v,i)=>Number.isFinite(v)&&Math.abs(v-b[i])<1e-4);
 for(const name of ['firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand']){
  const a=m.display[name],b=model.display[name];assert(a&&b,name+' missing');
  for(const key of ['rotation','translation','scale']){
   const def=key==='scale'?[1,1,1]:[0,0,0];
   assert(near(a[key]||def,b[key]||def),name+' '+key+' calibration changed');
  }
 }
 const signature=e=>JSON.stringify([e.from,e.to,e.origin,e.rotation||[0,0,0],
 Object.entries(e.faces).sort(([a],[b])=>a.localeCompare(b)).map(([k,v])=>[k,v.uv,v.rotation||0,v.texture])]);
 assert.deepEqual(m.source_resolution,canonical.resolution);
 assert.deepEqual(m.source_cubes.map(signature).sort(),canonical.elements.map(signature).sort(),'Throwknife geometry/UV changed; rebuild workspace');
 // Validate the actual rig and the actual 20 weapon cubes, not just metadata.
 const cfg={archetype:'blood_throwing_dagger',grip:base.grip,display:model.display,fp_mount:base.firstperson_offset,fp_idle_offset:base.firstperson_idle_offset,makeId:(()=>{let i=0;return ()=> 'check-'+(++i)})()};
 const expected=build(p,canonical,cfg,{width:64,height:32,itemWidth:32,itemHeight:32,textureIds:[0],source:'data:image/png;base64,test'});
 assert(build.compatibleMetadata(p,expected.toi_workspace),'Scene differs from canonical rig');
 const pattern=/_sword_cube_\d+$/;
 const cubes=p.elements.filter(e=>pattern.test(e.name));
 assert.equal(cubes.length,expected.elements.filter(e=>pattern.test(e.name)).length);
 for(const e of expected.elements.filter(e=>pattern.test(e.name))){
  const a=cubes.find(x=>x.name===e.name);assert(a,e.name);
  for(const k of ['from','to','origin','rotation'])assert(near(a[k]||[0,0,0],e[k]||[0,0,0]),e.name+' '+k);
 }
 // Replace rounded serialized calibration with the exact verified canonical matrices in a working copy.
 const scene={...p,toi_workspace:expected.toi_workspace};
 const allowed=new Set(['throw','blood_throw']);
 const animations={},report=[];
 for(const a of p.animations||[]){
  const raw=a.name.replace(/^animation\.toi\./,'').replace(/^animation\.blood_throwing_dagger\./,'');
  const name=raw;
  if(!allowed.has(name)){report.push({name:raw,skipped:true});continue;}
  const r=build.bakeGame(scene,a,'bb_author_'+name,60);
  assert(r.animations['bb_author_'+name],'Right hand required: '+name);
  assert(r.animations['bb_author_'+name+'_off'],'Both hands required: '+name);
  for(const [key,value] of Object.entries(r.animations)){
   assert(!animations[key],'Duplicate '+key);
   value.third={...value.third,baked:true};
   animations[key]=value;
  }
  report.push({name:raw,game:name,seconds:a.length,samples:r.samples,autoF5:r.autoF5,rotationError:r.maxRotationError,gripError:r.maxGripError});
 }
 assert(Object.keys(animations).length,'No recognized throwknife clips');
 return {animations,report};
}
module.exports={importThrowknife};
if(require.main===module){
 const root=path.resolve(__dirname,'../..'),assets=path.join(root,'src/main/resources/assets/tears-of-ink');
 const source=process.argv[2];if(!source)throw Error('Usage: node tools/blockbench/import_blood_throwing_dagger.js <blood_throwing_dagger_animation.bbmodel> [--write|--check]');
 const read=f=>JSON.parse(fs.readFileSync(f,'utf8'));
 const r=importThrowknife(read(source),read(path.join(assets,'models/item/blood_throwing_dagger.bbmodel')),read(path.join(assets,'weapon_anims/blood_throwing_dagger.json')),read(path.join(assets,'models/item/blood_throwing_dagger.json')));
 for(const row of r.report)console.log(row.skipped?'Skipped '+row.name:row.name+' -> '+row.game+': '+row.seconds+' s, '+row.samples+' samples; F5 '+(row.autoF5?'generated':'authored'));
 const out=path.join(assets,'weapon_anims/blood_throwing_dagger_bb_author.json');
 const payload={animations:r.animations};
 if(process.argv.includes('--write'))fs.writeFileSync(out,JSON.stringify(payload)+'\n');
 else if(process.argv.includes('--check')){
  assert.equal(r.report.filter(x=>!x.skipped).length,2,'Both authored clips required');
  assert.deepEqual(read(out),payload,'Author animation library is stale');
  console.log('Both throwknife clips verified against the game resource');
 }else console.log('Dry-run; no game files changed. Add --write to import.');
}
