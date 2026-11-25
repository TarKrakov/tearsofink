// Exact export of the author's original pistol. Run from any directory with Node.
// No author source is overwritten; validate both derived files with --check.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
function convert(source){
 const copy=x=>JSON.parse(JSON.stringify(x));
 const j=copy(source);
 // FaceInfo corner order in vanilla Java. Quarter turns preserve the four UV corners.
 const faces={down:[[0,0,1],[0,0,0],[1,0,0],[1,0,1]],up:[[0,1,0],[0,1,1],[1,1,1],[1,1,0]],north:[[1,1,0],[1,0,0],[0,0,0],[0,1,0]],south:[[0,1,1],[0,0,1],[1,0,1],[1,1,1]],west:[[0,1,0],[0,0,0],[0,0,1],[0,1,1]],east:[[1,1,1],[1,0,1],[1,0,0],[1,1,0]]};
 const corner=(e,v)=>v.map((n,i)=>n?e.to[i]:e.from[i]);
 const round=x=>Math.round(x*1e8)/1e8;
 function rotate(v,r,p){let [x,y,z]=v.map((n,i)=>n-p[i]);let c,s;for(let i=0;i<3;i++){let a=r[i]*Math.PI/180;c=Math.cos(a);s=Math.sin(a);if(i===0)[y,z]=[y*c-z*s,y*s+z*c];if(i===1)[x,z]=[x*c+z*s,-x*s+z*c];if(i===2)[x,y]=[x*c-y*s,x*s+y*c];}return [x,y,z].map((n,i)=>round(n+p[i]));}
 const equal=(a,b)=>a.every((x,i)=>Math.abs(x-b[i])<1e-7);
 for(const e of j.elements){
   const old=copy(e),r=old.rotation||[0,0,0];
   if(r.every(a=>a%90===0)&&r.some(a=>a!==0)){
     const points=Object.values(faces).flat().map(v=>rotate(corner(old,v),r,old.origin));
     e.from=[0,1,2].map(i=>Math.min(...points.map(v=>v[i])));e.to=[0,1,2].map(i=>Math.max(...points.map(v=>v[i])));e.rotation=[0,0,0];e.faces={};
     for(const [name,oldFace] of Object.entries(old.faces)){
       const transformed=faces[name].map(v=>rotate(corner(old,v),r,old.origin));
       const dest=Object.keys(faces).find(k=>faces[k].map(v=>corner(e,v)).every(v=>transformed.some(w=>equal(v,w))));
       if(!dest)throw Error("face missing "+name);
       const perm=faces[dest].map(v=>transformed.findIndex(w=>equal(w,corner(e,v))));
       if(!perm.every((n,i)=>n===(perm[0]+i)%4))throw Error("UV orientation "+perm);
       const next=copy(oldFace);const shift=((oldFace.rotation||0)+90*perm[0])%360;
       if(shift)next.rotation=shift;else delete next.rotation;e.faces[dest]=next;
     }
   }else if(r.filter(a=>a!==0).length>1)throw Error("non-quarter compound rotation");
 }
 for(const view of ["firstperson","thirdperson"]){
   const d=j.display[view+"_righthand"];if(!d)throw Error("right display missing");
   j.display[view+"_lefthand"]=copy(d); // Minecraft applies mirrored left transform (x translation, y/z rotation).
 }
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
