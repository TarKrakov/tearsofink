// Import Save for game from the calibrated holy_pistol workspace.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {bakeGame}=require('./toi_workspace.js');
const root=path.resolve(__dirname,'../..'),assets=path.join(root,'src/main/resources/assets/tears-of-ink');
const source=process.argv[2];if(!source)throw Error('Usage: node tools/blockbench/import_holy_pistol.js <holy_pistol_animation.bbmodel> [--write]');
const p=JSON.parse(fs.readFileSync(source,'utf8')),m=p.toi_workspace;
const base=JSON.parse(fs.readFileSync(path.join(assets,'weapon_anims/holy_pistol.json'),'utf8'));
const model=JSON.parse(fs.readFileSync(path.join(assets,'models/item/holy_pistol.json'),'utf8'));
assert.equal(m?.archetype,'holy_pistol');assert.deepEqual(m.grip,base.grip);
assert.deepEqual(m.fp_mount,base.firstperson_offset);assert.deepEqual(m.fp_idle_offset,base.firstperson_idle_offset);
for(const name of ['firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'])
 assert.deepEqual(m.display[name],model.display[name]);
const allowed=new Set(['aim_enter','aim_exit','shoot','shoot_aimed','reload','wild_shot','dual_aim_enter','dual_aim_exit','dual_shoot','dual_shoot_aimed','dual_reload']);
const animations={};
for(const a of p.animations||[]){
 const name=a.name.replace(/^animation\.toi\./,'').replace(/^animation\.holy_pistol\./,'');
 if(!allowed.has(name)){console.log('Skipped '+a.name);continue;}
 const r=bakeGame(p,a,'bb_author_'+name,60);
 if(!r.animations['bb_author_'+name])throw Error('Right hand required: '+name);
 if(name.startsWith('dual_')&&!r.animations['bb_author_'+name+'_off'])throw Error('Both hands required: '+name);
 for(const [key,value] of Object.entries(r.animations)){
  if(animations[key])throw Error('Duplicate '+key);
  value.third={...value.third,baked:true};
  animations[key]=value;
 }
 console.log(name+': '+a.length+' s, '+r.samples+' samples; F5 '+(r.autoF5?'generated':'authored'));
}
if(!Object.keys(animations).length)throw Error('No recognized pistol clips');
if(process.argv.includes('--write'))fs.writeFileSync(path.join(assets,'weapon_anims/holy_pistol_bb_author.json'),JSON.stringify({animations},null,2)+'\n');
else console.log('Dry-run; no game files changed. Add --write to import.');
