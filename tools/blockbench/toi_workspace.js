/* Tears of Ink artist workspace. Editor-only: requires Cameras by JannisX11. */
(function () {
  const projectMetadata=new WeakMap();
  let bar, frame, actions = [], callbacks = [], frameObserver;
  // Same exact quarter-turn bake as the registered pistol model export.
  function normalizePistolSource(source){
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
 return j;
}

  function buildWeaponProject(template,source,cfg,atlas) {
 if(cfg.archetype==='holy_pistol')source=normalizePistolSource(source);
 const cp=x=>JSON.parse(JSON.stringify(x)), fail=m=>{throw Error(m)}, vec=(v,n)=>{if(!Array.isArray(v)||v.length!==3||v.some(x=>typeof x!=='number'||!Number.isFinite(x)))fail('Invalid '+n);return v;};
 const add=(a,b)=>a.map((x,i)=>x+b[i]),sub=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,k)=>a.map(x=>x*k);
 const mm=(a,b)=>a.map(row=>b[0].map((_,j)=>row.reduce((v,x,k)=>v+x*b[k][j],0))),mv=(a,v)=>a.map(row=>row.reduce((z,x,i)=>z+x*v[i],0));
 const rx=a=>[[1,0,0],[0,Math.cos(a),-Math.sin(a)],[0,Math.sin(a),Math.cos(a)]],ry=a=>[[Math.cos(a),0,Math.sin(a)],[0,1,0],[-Math.sin(a),0,Math.cos(a)]],rz=a=>[[Math.cos(a),-Math.sin(a),0],[Math.sin(a),Math.cos(a),0],[0,0,1]];
 const rad=Math.PI/180,xyz=([x,y,z])=>mm(mm(rx(x*rad),ry(y*rad)),rz(z*rad));
 const euler=R=>{let y=Math.asin(Math.max(-1,Math.min(1,-R[2][0]))),x,z;if(Math.abs(Math.cos(y))>1e-7){x=Math.atan2(R[2][1],R[2][2]);z=Math.atan2(R[1][0],R[0][0]);}else{x=Math.atan2(-R[1][2],R[1][1]);z=0;}return [x/rad,y/rad,z/rad];};
 const A=mm([[-1,0,0],[0,-1,0],[0,0,1]],rx(-Math.PI/10)),C=mm(rx(-Math.PI/2),ry(Math.PI));
 const guide=delta=>{const L=Math.hypot(...delta);if(L<.01)fail('Grip coincides with shoulder');const d=mul(delta,1/L),v=[-d[2],0,d[0]],c=-d[1],K=[[0,-v[2],v[1]],[v[2],0,-v[0]],[-v[1],v[0],0]],K2=mm(K,K);return {L,R:euler(c>-.99999?K.map((row,i)=>row.map((x,j)=>(i===j?1:0)+x+K2[i][j]/(1+c))):rx(Math.PI))};};
 const p=cp(template),grip=vec(cfg.grip,'grip'),mount=vec(cfg.fp_mount,'FP offset'),idle=vec(cfg.fp_idle_offset,'FP idle');
 if(!/^[a-z0-9_]+$/.test(cfg.archetype))fail('Invalid weapon animation ID');
 if(source.meta?.model_format!=='java_block')fail('Select an original Java item .bbmodel');
 if(!source.elements?.length||source.elements.length>2000)fail('Unsupported item size');
 if((source.groups||[]).some(g=>(g.rotation||[]).some(n=>Math.abs(n)>1e-8)))fail('Bake rotated groups into cubes first');
 const raw=source.elements.filter(e=>e.export!==false);
 if(!raw.length)fail('Нет экспортируемых кубов');
 const bad=raw.find(e=>e.type!=='cube'||e.box_uv||e.rescale||(e.inflate||0)!==0||(e.rotation||[]).filter(n=>Math.abs(n)>1e-8).length>1);
 if(bad)fail('Неподдерживаемый куб: '+(bad.name||bad.uuid)+'; rotation '+JSON.stringify(bad.rotation||[0,0,0])+'. Нужен Java cube с Face UV, без inflate/rescale и со вращением по одной оси.');
 if(!source.resolution||source.resolution.width<=0||source.resolution.height<=0)fail('Invalid UV resolution');
 // Identical duplicate textures and identical cubes may occur after merging author models.
 // Normalize only the generated copy; never discard any distinct geometry or UV.
 const originals=[],faces=[],seen=new Set();
 for(const el of raw){
  const ff=cp(el.faces);
  for(const f of Object.values(ff)){
   if(f.texture===null||f.texture===false)continue;
   if(!atlas.textureIds.includes(f.texture))fail('Unsupported face texture');
   if(!Array.isArray(f.uv)||f.uv.length!==4||f.uv.some(x=>!Number.isFinite(x)))fail('Invalid UV');
   f.uv=f.uv.map((x,i)=>x*(i%2?atlas.itemHeight/source.resolution.height:atlas.itemWidth/source.resolution.width));f.texture=0;
  }
  const sig=JSON.stringify([el.from,el.to,el.origin,el.rotation||[0,0,0],Object.entries(ff).sort(([a],[b])=>a.localeCompare(b)).map(([k,v])=>[k,v.uv,v.rotation||0,v.texture])]);
  if(!seen.has(sig)){seen.add(sig);originals.push(el);faces.push(ff);}
 }
 const old=new Set(p.elements.filter(e=>/_sword_cube_\d+$/.test(e.name)).map(e=>e.uuid));p.elements=p.elements.filter(e=>!old.has(e.uuid));
 const groups=Object.fromEntries(p.groups.map(g=>[g.name,g])),nodes={};
 function walk(arr){for(let i=arr.length-1;i>=0;i--){const v=arr[i];if(typeof v==='string'){if(old.has(v))arr.splice(i,1);}else{nodes[v.uuid]=v;walk(v.children);}}}walk(p.outliner);
 for(const g of p.groups){g.rotation=[0,0,0];g.visibility=g.name.startsWith('F5_');g.selected=false;}
 for(const e of p.elements){e.visibility=e.name.startsWith('F5_')&&e.type!=='camera';if(e.type==='camera'){e.linked_preview='';e.camera_linked=false;continue;}if(e.type!=='cube')fail('Unexpected rig element');
 const tile=/_forearm$/.test(e.name)?(e.name.includes('_left_')?4:3):/_leg$/.test(e.name)?2:e.name==='F5_torso'?1:0,x=atlas.itemWidth+(tile%4)*8,y=Math.floor(tile/4)*8;for(const f of Object.values(e.faces)){f.uv=[x+.2,y+.2,x+7.8,y+7.8];f.texture=0;}}
 const bindings={};
 for(const view of ['FP','F5'])for(const side of ['right','left']){
 const sign=side==='right'?1:-1,b=view+'_'+side,shoulder=b+'_shoulder',arm=b+'_arm',sword=b+'_sword';
 const d=cfg.display[(view==='FP'?'firstperson_':'thirdperson_')+side+'hand'];if(!d)fail('Missing Display for '+b);
 const r=vec(d.rotation||[0,0,0],'Display rotation'),t=vec(d.translation||[0,0,0],'Display translation'),ds=vec(d.scale||[1,1,1],'Display scale'),scale=ds[0];
 if(scale<=0||ds.some(v=>Math.abs(v-scale)>1e-8))fail('Positive uniform Display scale required');
 const R=xyz([r[0],sign*r[1],sign*r[2]]),G=add(mv(R,mul(sub(grip,[8,8,8]),scale)),[sign*t[0],t[1],t[2]]);
 let P,H,baseR;
 if(view==='FP'){const off=add(mount,idle);H=add(add([sign*.56*16,-.52*16,-.72*16],[sign*off[0],off[1],off[2]]),G);P=add(H,[0,10,4]);baseR=R;}
 else{P=[sign*5,22.016,0];baseR=mm(mm(A,C),R);H=add(P,mv(A,mv(C,add([sign,2,-10],G))));}
 if(!groups[shoulder]||!groups[arm]||!groups[sword]||!nodes[groups[sword].uuid])fail('Open a TOI weapon rig');
 groups[shoulder].origin=P;groups[arm].origin=P;groups[sword].origin=H;groups[sword].rotation=euler(baseR);
 const fore=p.elements.find(e=>e.name===b+'_forearm'),palm=p.elements.find(e=>e.name===b+'_palm');if(!fore||!palm)fail('Missing hand guides');
 const shape=guide(sub(H,P));Object.assign(fore,{origin:P,from:add(P,[-1.7,-shape.L,-1.7]),to:add(P,[1.7,0,1.7]),rotation:shape.R});Object.assign(palm,{origin:H,from:add(H,[-1.6,-1.6,-1.6]),to:add(H,[1.6,1.6,1.6]),rotation:[0,0,0]});
 originals.forEach((original,i)=>{const el=cp(original),id=cfg.makeId();Object.assign(el,{name:sword+'_cube_'+i,uuid:id,locked:true,visibility:view==='F5',export:true,faces:cp(faces[i])});for(const k of ['from','to','origin'])el[k]=add(H,mul(sub(vec(original[k],k),grip),scale));el.rotation=original.rotation||[0,0,0];p.elements.push(el);nodes[groups[sword].uuid].children.push(id);});
 bindings[b]={arm,shoulder,sword,pivot:P,grip_position:H,item_rotation:baseR,scale};
 }
 p.name=cfg.archetype+'_animation';p.model_identifier='toi_weapon_workspace_v2';p.resolution={width:atlas.width,height:atlas.height};
 const tex={...p.textures[0],source:atlas.source,name:'TOI_editor_atlas.png',path:'',mode:'bitmap',id:'0',width:atlas.width,height:atlas.height,uv_width:atlas.width,uv_height:atlas.height,internal:true,saved:true,layers_enabled:false};delete tex.relative_path;delete tex.layers;
 p.textures=[tex];p.animations=[];delete p.history;delete p.animation_controllers;delete p.backgrounds;
 p.editor_state={mode:'edit',tool:'rotate_tool',selected_elements:[],selected_groups:[],previews:{main:{position:[45,30,55],target:[0,16,0],orthographic:false,zoom:1}}};
 p.toi_workspace={version:2,archetype:cfg.archetype,euler_order:'ZYX',grip,bindings,display:cp(cfg.display),camera_names:['TOI_FP_70','TOI_F5_front','TOI_F5_back'],fp_mount:mount,fp_idle_offset:idle,source_name:source.name||cfg.archetype,source_resolution:cp(source.resolution),source_cubes:originals.map((e,i)=>({from:e.from,to:e.to,origin:e.origin,rotation:e.rotation||[0,0,0],faces:e.faces,atlas_faces:faces[i]})),calibration:'renderer math only; live Minecraft comparison pending'};
 return p;
}

  // Default Claude solve4/base4 (tools/preview/third.py), expressed directly in
  // editor world coordinates. Only the generated F5 tracks are changed.
  const f5math=(()=>{
    const rad=Math.PI/180, clamp=(x,a,b)=>Math.max(a,Math.min(b,x));
    const add=(a,b)=>a.map((x,i)=>x+b[i]),sub=(a,b)=>a.map((x,i)=>x-b[i]),mul=(a,s)=>a.map(x=>x*s);
    const mm=(a,b)=>a.map(r=>b[0].map((_,j)=>r.reduce((s,x,k)=>s+x*b[k][j],0)));
    const mv=(a,b)=>a.map(r=>r.reduce((s,x,k)=>s+x*b[k],0));
    const tr=a=>a[0].map((_,i)=>a.map(r=>r[i]));
    const rx=x=>[[1,0,0],[0,Math.cos(x),-Math.sin(x)],[0,Math.sin(x),Math.cos(x)]];
    const ry=x=>[[Math.cos(x),0,Math.sin(x)],[0,1,0],[-Math.sin(x),0,Math.cos(x)]];
    const rz=x=>[[Math.cos(x),-Math.sin(x),0],[Math.sin(x),Math.cos(x),0],[0,0,1]];
    const rot=([x,y,z])=>mm(mm(rz(z*rad),ry(y*rad)),rx(x*rad));
    const F=[[-1,0,0],[0,-1,0],[0,0,1]];
    const err=s=>{throw Error(s)};
    const number=x=>{if((typeof x!=='number'&&typeof x!=='string')||String(x).trim()===''||!Number.isFinite(Number(x)))err('Только числовые ключи, без Molang / expressions');return Number(x)};
    const vec=(a,d=0)=>{const v=Array.isArray(a)?a:['x','y','z'].map(k=>a?.[k]??d);if(v.length!==3)err('Неверный вектор рига');return v.map(number)};
    const norm=a=>{const l=Math.hypot(...a);if(l<1e-9)err('Направление руки не определено');return mul(a,1/l)};
    const near=(a,b)=>a.every((x,i)=>Math.abs(x-b[i])<1e-5);
    const unwrap=(a,b)=>a.map((x,i)=>x+360*Math.round((b[i]-x)/360));
    // Same ZYX convention as Rig.pose; choose the continuous Euler branch,
    // including the alternate equivalent solution near gimbal lock.
    function euler(R,prev){
      const y=Math.asin(clamp(-R[2][0],-1,1));let x,z;
      if(Math.abs(Math.cos(y))>1e-7){x=Math.atan2(R[2][1],R[2][2]);z=Math.atan2(R[1][0],R[0][0]);}
      else{z=prev?prev[2]*rad:0;x=y>0?Math.atan2(R[0][1],R[0][2])+z:Math.atan2(-R[0][1],-R[0][2])-z;}
      let a=[x/rad,y/rad,z/rad];if(!prev)return a;
      const b=unwrap([a[0]+180,180-a[1],a[2]+180],prev);a=unwrap(a,prev);
      return Math.hypot(...sub(a,prev))<=Math.hypot(...sub(b,prev))?a:b;
    }
    const timeKey=t=>Number(t.toFixed(8));
    const key=(t,v,interpolation='linear')=>({channel:'rotation',time:timeKey(t),interpolation,data_points:[{x:String(v[0]),y:String(v[1]),z:String(v[2])}]});
    function channels(a,length){
      const result={};
      if(a?.rotation_global||a?.quaternion_interpolation)err('Сначала запеки global/quaternion в локальные Euler-ключи');
      for(const k of a?.keyframes||[]){
        if(!['rotation','position','scale'].includes(k.channel))err('Неподдерживаемый канал '+k.channel);
        const t=number(k.time),kind=k.interpolation||'linear';
        if(t<0||t>length+1e-8||k.data_points?.length!==1)err('Ключ вне длительности или split-key: сначала запеки split-ключи');
        if(!['linear','step','catmullrom','bezier'].includes(kind))err('Неподдерживаемая интерполяция '+kind);
        const v=vec(k.data_points[0],k.channel==='scale'?1:0);
        if(k.channel==='scale'&&(!near(v,[1,1,1])||kind==='bezier'))err('Анимация масштаба не поддерживается');
        const entry={...k,time:t,kind,v};
        if(kind==='bezier'){
          for(const f of ['bezier_left_time','bezier_right_time','bezier_left_value','bezier_right_value'])
            entry[f]=vec(k[f]??(f.endsWith('_time')?(f.includes('left')?[-.1,-.1,-.1]:[.1,.1,.1]):[0,0,0]));
        }
        (result[k.channel]??=[]).push(entry);
      }
      for(const keys of Object.values(result)){keys.sort((a,b)=>a.time-b.time);if(keys.some((k,i)=>i&&Math.abs(k.time-keys[i-1].time)<1e-9))err('Два ключа одного канала в одно время');}
      return result;
    }
    function sample(keys,t,def=0){
      if(!keys?.length)return [def,def,def];
      if(t<=keys[0].time)return keys[0].v;if(t>=keys.at(-1).time)return keys.at(-1).v;
      const i=keys.findIndex(k=>k.time>t)-1,a=keys[i],b=keys[i+1],dt=b.time-a.time,u=(t-a.time)/dt,p=a.v,q=b.v;
      if(a.kind==='step')return p;
      if(a.kind==='linear'&&['linear','step'].includes(b.kind))return add(p,mul(sub(q,p),u));
      if(a.kind==='catmullrom'||b.kind==='catmullrom'){
        const p0=keys[i-1]?.v||p,p3=keys[i+2]?.v||q;
        return p.map((x,j)=>.5*(2*x+(-p0[j]+q[j])*u+(2*p0[j]-5*x+4*q[j]-p3[j])*u*u+(-p0[j]+3*x-3*q[j]+p3[j])*u*u*u));
      }
      // Same monotonic Bezier-time inversion as import_weapon_animation.py.
      return p.map((x,j)=>{
        const r=clamp((a.bezier_right_time?.[j]??.1)/dt,0,1),l=1+clamp((b.bezier_left_time?.[j]??-.1)/dt,-1,0);
        let lo=0,hi=1;for(let k=0;k<40;k++){const v=(lo+hi)/2,w=1-v;if(3*w*w*v*r+3*w*v*v*l+v*v*v<u)lo=v;else hi=v;}
        const v=(lo+hi)/2,w=1-v;
        return w*w*w*x+3*w*w*v*(x+(a.bezier_right_value?.[j]??0))+3*w*v*v*(q[j]+(b.bezier_left_value?.[j]??0))+v*v*v*q[j];
      });
    }
    function rig(project,clip,allTracks=false){
      const meta=project.toi_workspace;
      if(!meta||![1,2].includes(meta.version)||meta.euler_order!=='ZYX')err('Не удалось подтвердить калибровку этой сцены. Не удаляй анимации: сохрани копию .bbmodel и передай её для восстановления');
      const length=number(clip.length);
      if(length<=0||length>60||!['once','hold','loop'].includes(clip.loop||'once'))err('Нужна анимация длиной 0–60 секунд');
      if(clip.anim_time_update||clip.start_delay||clip.loop_delay||clip.blend_weight)err('Сначала убери выражения времени, задержки и blend weight');
      const groups=new Map(project.groups.map(g=>[g.name,g])),byId=new Map(project.groups.map(g=>[g.uuid,g])),parents=new Map();
      if(groups.size!==project.groups.length)err('Повторяющиеся имена костей');
      function walk(list,parent=null){for(const node of list)if(typeof node==='object'){if(!byId.has(node.uuid)||parents.has(node.uuid))err('Некорректная иерархия рига');parents.set(node.uuid,parent);walk(node.children||[],node.uuid)}}
      walk(project.outliner);if(parents.size!==groups.size)err('Кость отсутствует в Outliner');
      const get=n=>{const g=groups.get(n);if(!g)err('Отсутствует кость '+n);return g};
      const valid=new Set(),sides=[];
      for(const side of ['right','left']){
        const pair={side,sign:side==='right'?1:-1};
        for(const view of ['FP','F5']){
          const b=meta.bindings?.[view+'_'+side];if(!b)err('Нет калибровки '+view+' '+side);
          const chain=['shoulder','arm','sword'].map(k=>get(b[k]));
          if(parents.get(chain[1].uuid)!==chain[0].uuid||parents.get(chain[2].uuid)!==chain[1].uuid)err('Изменена иерархия рига');
          chain.forEach((g,i)=>{valid.add(g.uuid);if(!near(vec(g.origin),vec(i===2?b.grip_position:b.pivot)))err('Изменён pivot '+g.name);if(i<2&&!near(vec(g.rotation),[0,0,0]))err('Восстанови исходную нейтраль '+g.name);});
          const R=rot(vec(chain[2].rotation));
          if(b.item_rotation&&!R.every((r,i)=>near(r,b.item_rotation[i])))err('Изменена нейтраль меча '+chain[2].name);
          pair[view]={b,chain};
        }
        sides.push(pair);
      }
      const tracks=clip.animators||{},fpChannels=new Map(),times=new Set([0,length]),jumps=new Set();
      for(const [id,a] of Object.entries(tracks)){
        if(!a.keyframes?.length)continue;
        const g=byId.get(id);
        if(!g||!valid.has(id))err('Анимируй только shoulder/arm/sword; неподдерживаемый трек '+(g?.name||a.name||id));
        if(!allTracks&&!g.name.startsWith('FP_'))continue;
        const cs=channels(a,length);fpChannels.set(id,cs);
        for(const ks of Object.values(cs))ks.forEach((k,i)=>{times.add(k.time);if(i&&ks[i-1].kind==='step')jumps.add(k.time)});
      }
      function pose(g,t,cache){
        if(cache.has(g.uuid))return cache.get(g.uuid);
        const pid=parents.get(g.uuid),p=pid?byId.get(pid):null,cs=fpChannels.get(g.uuid)||{};
        const o=add(sub(vec(g.origin),p?vec(p.origin):[0,0,0]),sample(cs.position,t)),R=rot(add(vec(g.rotation),sample(cs.rotation,t)));
        const P=p?pose(p,t,cache):null,poseValue=P?{R:mm(P.R,R),o:add(P.o,mv(P.R,o))}:{R,o};
        cache.set(g.uuid,poseValue);return poseValue;
      }
      function tipLocal(b){
        // Java cube corners, including their original single-axis rotations.
        // The editor cubes are already scaled and relative to the weapon grip.
        let best=null,d=-1;
        for(const el of project.elements.filter(e=>e.name?.startsWith(b.sword+'_cube_'))){
          const a=vec(el.from),z=vec(el.to),o=vec(el.origin),R=rot(vec(el.rotation));
          for(const x of [a[0],z[0]])for(const y of [a[1],z[1]])for(const w of [a[2],z[2]]){
            const q=sub(add(o,mv(R,sub([x,y,w],o))),vec(b.grip_position)),dist=q.reduce((s,k)=>s+k*k,0);
            if(dist>d){best=q;d=dist;}
          }
        }
        if(d<1e-8)err('Нет геометрии для определения кончика клинка');return best;
      }
      return {meta,length,sides,tracks,fpChannels,times,jumps,pose,tipLocal};
    }
    function existingF5(project,clip){const ids=new Set(project.groups.filter(g=>g.name.startsWith('F5_')).map(g=>g.uuid));return Object.entries(clip.animators||{}).some(([id,a])=>ids.has(id)&&a.keyframes?.length)}
    function planF5(project,clip,replace=false){
      if(!replace&&existingF5(project,clip))return {status:'protected',animators:{}};
      const r=rig(project,clip),active=r.sides.filter(s=>s.FP.chain.some(g=>r.fpChannels.has(g.uuid)));
      if(!active.length)return {status:'empty',animators:{}};
      const fps=clamp(number(clip.snapping||20),20,60);
      const times=new Set([...r.times].map(timeKey)),beforeJump=new Set();
      for(let i=0;i/fps<r.length;i++)times.add(timeKey(i/fps));
      for(const t of r.jumps){const p=timeKey(Math.max(0,t-0.00001));if(p<t){times.add(p);beforeJump.add(p)}}
      const sorted=[...times].sort((a,b)=>a-b),patch={};
      for(const side of active){
        const sign=side.sign,[shoulder,arm,sword]=side.F5.chain,tip=r.tipLocal(side.FP.b),mount=vec(r.meta.fp_mount),off=[sign*mount[0],mount[1],mount[2]];
        const armKeys=[],swordKeys=[];let prevArm=vec(arm.rotation),prevSword=vec(sword.rotation);
        for(const t of sorted){
          const p=r.pose(side.FP.chain[2],t,new Map());
          const h=mul(sub(p.o,off),1/16),Q=add(h,mul(mv(p.R,tip),.5/16));
          const v=norm(sub(Q,[sign*5/16,-.245,0]).map((x,i)=>x*(i===2?1:1.3)));
          const d=mv(F,v),x=-Math.acos(clamp(d[1],-1,1)),y=Math.abs(Math.sin(x))>1e-3?Math.atan2(-d[0],-d[2]):0;
          const A=mm(ry(y),rx(x));
          // Importer's inverse: A = F^T * arm_world * F * Rx(-18°).
          const armR=mm(mm(mm(F,A),rx(Math.PI/10)),F);
          // Claude wrist: W = inv(A*C) * F * FP_orientation * inv(TP_Display).
          // In editor world coordinates the resulting sword orientation is FP_orientation.
          const swordR=mm(tr(armR),p.R);
          prevArm=euler(armR,prevArm);prevSword=euler(swordR,prevSword);
          const kind=beforeJump.has(t)?'step':'linear';
          armKeys.push(key(t,sub(prevArm,vec(arm.rotation)),kind));
          swordKeys.push(key(t,sub(prevSword,vec(sword.rotation)),kind));
        }
        for(const [g,keys] of [[shoulder,[]],[arm,armKeys],[sword,swordKeys]])
          patch[g.uuid]={name:g.name,type:'bone',rotation_global:false,quaternion_interpolation:false,keyframes:keys};
      }
      return {status:'generated',animators:patch,sides:active.map(s=>s.side),samples:sorted.length};
    }
    function planTiming(project,clip){
      const r=rig(project,clip),patch={},skipped=[];let inserted=0;
      if(!r.fpChannels.size)return {status:'empty',animators:{},inserted,skipped};
      for(const side of r.sides)for(const g of side.F5.chain){
        const a=r.tracks[g.uuid];if(!a?.keyframes?.length)continue;
        // Never refit a hand-authored curve. Splitting a linear channel at a
        // sampled value is exact; Bezier/Catmull/split/quaternion are left alone.
        let ks;try{ks=channels(a,r.length).rotation}catch(e){skipped.push(g.name);continue}
        if(!ks?.length||ks.some(k=>k.kind!=='linear')){skipped.push(g.name);continue;}
        const extras=[];
        for(const t of [...r.times].sort((a,b)=>a-b)){
          if(ks.some(k=>Math.abs(k.time-t)<1e-7))continue;
          extras.push(key(t,sample(ks,t)));
        }
        if(extras.length){patch[g.uuid]={...a,keyframes:[...a.keyframes,...extras]};inserted+=extras.length;}
      }
      return {status:inserted?'synced':'unchanged',animators:patch,inserted,skipped};
    }

    // Game -> editor: standalone renderer channels in calibrated rig. No server tempo/transition.
    const I=()=>[[1,0,0],[0,1,0],[0,0,1]],xyz=([x,y,z])=>mm(mm(rx(x*rad),ry(y*rad)),rz(z*rad)),C=mm(rx(-Math.PI/2),ry(Math.PI)),SX=[[-1,0,0],[0,1,0],[0,0,1]];
    const mix=(a,b,u)=>add(a,mul(sub(b,a),u));
    function ease(k,u){u=clamp(u,0,1);switch(String(k||'ease_in_out').toLowerCase()){case'linear':return u;case'ease_in':return u**3;case'ease_out':return 1-(1-u)**3;case'heavy':return u**4;case'back_out':return 1+2.70158*(u-1)**3+1.70158*(u-1)**2;default:return u<.5?4*u**3:1-(-2*u+2)**3/2;}}
    function axisR(a,t){const[x,y,z]=norm(a),c=Math.cos(t),s=Math.sin(t),v=1-c;return [[c+x*x*v,x*y*v-z*s,x*z*v+y*s],[y*x*v+z*s,c+y*y*v,y*z*v-x*s],[z*x*v-y*s,z*y*v+x*s,c+z*z*v]];}
    function fraction(R,u){if(u<=0)return I();if(u>=1)return R;const a=Math.acos(clamp((R[0][0]+R[1][1]+R[2][2]-1)/2,-1,1));if(a<1e-7)return I();let v=[R[2][1]-R[1][2],R[0][2]-R[2][0],R[1][0]-R[0][1]];if(Math.abs(Math.PI-a)<1e-5){const i=[0,1,2].reduce((b,k)=>R[k][k]>R[b][b]?k:b,0);v=[0,1,2].map(k=>R[k][i]+(k===i?1:0));}return axisR(v,a*u);}
    const mixR=(a,b,u)=>mm(a,fraction(mm(tr(a),b),u));
    const xyzAngles=R=>Math.abs(R[0][2])<.99999999?[Math.atan2(-R[1][2],R[2][2])/rad,Math.asin(clamp(R[0][2],-1,1))/rad,Math.atan2(-R[0][1],R[0][0])/rad]:[Math.atan2(R[2][1],R[1][1])/rad,Math.sign(R[0][2])*90,0];
    const gameEnd=c=>c.hold>0&&c.keyframes.length?c.keyframes.at(-1).t+c.hold+(c.return??.3):c.length;
    function checkedClip(c,n){
      if(!c||!c.keyframes?.length||!Number.isFinite(c.length)||c.length<=0||c.length>60)err('Некорректный игровой клип '+n);
      if(c.absolute||c.keyframes.some(k=>k.scale&&!near(vec(k.scale,1),[1,1,1])))err('Absolute/scale не поддерживаются: '+n);
      const keys=c.keyframes.map(k=>({...k,t:number(k.t),rotation:vec(k.rotation),translation:vec(k.translation),ease:k.ease||'ease_in_out'})).sort((a,b)=>a.t-b.t);
      if(keys.some((k,i)=>k.t<0||k.t>60||i&&k.t<=keys[i-1].t))err('Неверные времена '+n);
      return {...c,keyframes:keys};
    }
    function gameSample(c,t){
      const rest={t:0,rotation:[0,0,0],translation:[0,0,0],ease:'ease_in_out'},ks=c.keyframes,last=ks.at(-1);
      const rm=(a,b,u)=>c.rotation_mode==='slerp'?xyzAngles(mixR(xyz(a),xyz(b),u)):mix(a,b,u);
      if(c.third?.baked&&t>=last.t)return last;
      if(c.hold>0&&t>=last.t){const u=ease('ease_in_out',(t-last.t-c.hold)/Math.max(.01,c.return??.3));return {rotation:rm(last.rotation,rest.rotation,u),translation:mix(last.translation,rest.translation,u)};}
      const pts=[...(ks[0].t>0?[rest]:[]),...ks,...(c.hold>0?[]:[{...rest,t:c.length}])];
      if(pts.length===1)return pts[0];
      let i=0;while(i<pts.length-2&&pts[i+1].t<=t)i++;
      const a=pts[i],b=pts[Math.min(i+1,pts.length-1)],raw=b.t>a.t?clamp((t-a.t)/(b.t-a.t),0,1):1,u=ease(a.ease,raw);
      function curve(f){
        if(!c.smooth)return mix(a[f],b[f],u);
        const p0=pts[Math.max(0,i-1)][f],p1=a[f],p2=b[f],p3=pts[Math.min(i+2,pts.length-1)][f];
        if(c.smooth!=='tight')return p1.map((x,j)=>.5*(2*x+(-p0[j]+p2[j])*u+(2*p0[j]-5*x+4*p2[j]-p3[j])*u*u+(-p0[j]+3*x-3*p2[j]+p3[j])*u*u*u));
        const h=a.ease==='ease_in_out'?raw:u,slope=(a,b)=>a*b<=0?0:Math.sign(a+b)*Math.min(Math.abs((a+b)/2),3*Math.min(Math.abs(a),Math.abs(b)));
        return p1.map((x,j)=>{const y=p2[j],s=slope(x-p0[j],y-x),z=slope(y-x,p3[j]-y);return s===0&&z===0?x+(y-x)*u:(2*h**3-3*h*h+1)*x+(h**3-2*h*h+h)*s+(-2*h**3+3*h*h)*y+(h**3-h*h)*z;});
      }
      let rotation=curve('rotation');
      if(c.rotation_mode==='slerp'){let a=rest,b={...rest,t:c.length};for(const k of ks){if(k.t>t){b=k;break}a=k;}rotation=rm(a.rotation,b.rotation,ease(a.ease,b.t>a.t?(t-a.t)/(b.t-a.t):1));}
      return {rotation,translation:curve('translation')};
    }
    const signed=(v,m)=>[m*v[0],v[1],v[2]],rel=(v,m)=>xyz([v[0],m*v[1],m*v[2]]);
    function importGame(project,library,requests,fps=30){
      const meta=project.toi_workspace,base=library.base;
      if(!meta||!base?.grip||!near(vec(meta.grip),vec(base.grip))||!near(vec(meta.fp_mount),vec(base.firstperson_offset))||!near(vec(meta.fp_idle_offset),vec(base.firstperson_idle_offset)))err('Игровой grip/offset не совпадает с оружием. Выбери его собственный основной JSON.');
      if(base.third?.centre)err('Импорт щитов пока не поддерживается');
      if(!Number.isFinite(fps)||fps<20||fps>60)err('Частота 20–60');
      const cache=new Map(),get=n=>{if(!cache.has(n))cache.set(n,checkedClip(library.animations[n],n));return cache.get(n)};
      const tune=base.third||{},out=[];
      for(const spec of requests){
        const names=[spec.right||'',spec.left||''];if(!names.some(Boolean))continue;
        const clips=names.map(n=>n?get(n):null),length=Math.max(...clips.filter(Boolean).map(c=>Math.max(c.length,c.keyframes.at(-1).t)));
        const r=rig(project,{length,animators:{},loop:'once'}),animators={},times=new Set([0,timeKey(length)]);
        const tips=r.sides.map(s=>r.tipLocal(s.FP.b));
        for(let i=0;i/fps<length;i++)times.add(timeKey(i/fps));
        clips.forEach((c,i)=>{if(!c)return;for(const k of c.keyframes)if(k.t<=length)times.add(timeKey(k.t));
          const src=c.third?.source?get(c.third.source):c,scale=names[i].startsWith('bb_author_')&&src!==c?c.length/src.length:1;
          for(const k of src.keyframes)if(k.t*scale<=length)times.add(timeKey(k.t*scale));
          if(c.third?.wrist_source)for(const k of get(c.third.wrist_source).keyframes)if(k.t<=length)times.add(timeKey(k.t));
        });
        for(let i=0;i<2;i++){
          const c=clips[i];if(!c)continue;
          const pair=r.sides[i],m=pair.sign,fix=c.third||{},src=fix.source?get(fix.source):c,auth=names[i].startsWith('bb_author_');
          if(!fix.arm_space&&(tune.push||fix.push||fix.push_right||fix.push_left))err('F5 push не поддерживается: '+names[i]);
          const fs=pair.FP.chain[2],ta=pair.F5.chain[1],ts=pair.F5.chain[2],fpB=pair.FP.b,idle=signed(vec(meta.fp_idle_offset),m);
          function basePose(index,clip,t){
            const s=r.sides[index].sign,b=r.sides[index].FP.b,k=gameSample(clip,t),D=b.item_rotation,R=mm(rel(k.rotation,s),D),tip=tips[index];
            const restH=mul(sub(vec(b.grip_position),signed(add(vec(meta.fp_mount),vec(meta.fp_idle_offset)),s)),1/16);
            const h=add(restH,mul(signed(k.translation,s),1/16));
            let Q=add(h,mul(mv(R,tip),(tune.lead??.5)/16));
            if((fix.lateral??1)!==1){const restX=restH[0]+mv(D,tip)[0]*(tune.lead??.5)/16;Q[0]=restX+fix.lateral*(Q[0]-restX);}
            Q[0]*=fix.center??1;if(fix.no_cross)Q[0]=s*Math.max(s*Q[0],.08);
            let d=mv(F,norm(sub(Q,[s*5/16,-.245,0]).map((v,j)=>v*(j===2?1:tune.reach??1.3))));
            const item=mm(F,R);if(fix.follow_blade)d=norm(add(mul(d,.25),mul(norm(mv(item,tip)),.75)));
            return {d,item};
          }
          const angle=d=>{const x=-Math.acos(clamp(d[1],-1,1));return [x,Math.abs(Math.sin(x))>1e-3?Math.atan2(-d[0],-d[2]):0]};
          const Tp=meta.display['thirdperson_'+pair.side+'hand'],Dtp=rel(vec(Tp.rotation),m);
          function third(t){
            const ptime=auth&&src!==c?Math.min(t,c.length)*src.length/Math.max(.01,c.length):t;
            let x,y,z=0,W=I();
            if(fix.arm_space){
              const k=gameSample(src,ptime);[x,y,z]=[-Math.PI/10+k.rotation[0]*rad,m*k.rotation[1]*rad,m*k.rotation[2]*rad];
              if(fix.wrist_source){const v=gameSample(get(fix.wrist_source),t).rotation;W=fix.wrist_xyz?xyz(v):axisR(mv(Dtp,tips[i]),v[0]*rad);}
            }else{
              const ci=fix.mirror&&m===-1?0:i;
              let b=basePose(ci,src,ptime),bw=(fix.wrist_hold??-1)>=0?basePose(ci,src,fix.wrist_hold):b;
              if(ci!==i){const mirror=v=>({d:mv(SX,v.d),item:mm(mm(mm(SX,v.item),SX),ry(Math.PI))});b=mirror(b);bw=mirror(bw);}
              [x,y]=angle(b.d);const [xw,yw]=angle(bw.d);
              W=mm(mm(mm(tr(mm(mm(ry(yw),rx(xw)),C)),ry(-m*(fix.turn??0)*rad)),bw.item),tr(Dtp));
            }
            const w=fix.baked?1:ease('ease_in_out',Math.min(t/(auth ? .09 : meta.archetype==='pale_linear_greatsword' ? .18 : .1), Math.max(0,(gameEnd(c)-t)/(c.hold>0?Math.max(.05,c.return??.3):.15))));
            const A=mm(mm(rz(z*w),ry(y*w)),rx(-Math.PI/10+(x+Math.PI/10)*w));
            return {arm:mm(mm(mm(F,A),rx(Math.PI/10)),F),sword:mm(mm(mm(mm(F,rx(-Math.PI/10)),C),fraction(W,w)),Dtp)};
          }
          const tracks=[[],[],[]],prev=[vec(fs.rotation),vec(ta.rotation),vec(ts.rotation)];
          for(const t of [...times].sort((a,b)=>a-b)){
            const k=gameSample(c,Math.min(t,gameEnd(c))),R=mm(rel(k.rotation,m),fpB.item_rotation);
            const idleStart=c.keyframes.at(-1).t+Math.max(0,c.hold||0),iw=ease('ease_in_out',(t-idleStart)/Math.max(.01,gameEnd(c)-idleStart));
            const delta=add(signed(k.translation,m),mul(idle,iw-1));
            prev[0]=euler(R,prev[0]);tracks[0].push({...key(t,delta),channel:'position'},key(t,sub(prev[0],vec(fs.rotation))));
            const p=third(Math.min(t,gameEnd(c)));prev[1]=euler(p.arm,prev[1]);prev[2]=euler(p.sword,prev[2]);
            tracks[1].push(key(t,sub(prev[1],vec(ta.rotation))));tracks[2].push(key(t,sub(prev[2],vec(ts.rotation))));
          }
          for(const [g,keys] of [[fs,tracks[0]],[ta,tracks[1]],[ts,tracks[2]]])animators[g.uuid]={name:g.name,type:'bone',rotation_global:false,quaternion_interpolation:false,keyframes:keys};
        }
        out.push({name:'game_'+(spec.label||names.find(Boolean)),length:timeKey(length),snapping:fps,loop:'once',animators});
      }
      return out;
    }
    function copyClip(source,clip,target){
      const allowed=/^(FP|F5)_(right|left)_(shoulder|arm|sword)$/,src=new Map(source.groups.map(g=>[g.uuid,g.name])),dst=new Map(target.groups.map(g=>[g.name,g.uuid])),animators={};
      for(const [id,a] of Object.entries(clip.animators||{})){
        if(!a.keyframes?.length)continue;const name=src.get(id);
        if(!name||!allowed.test(name)||!dst.has(name))err('Нельзя перенести трек '+(name||a.name||id));
        const cp=JSON.parse(JSON.stringify(a));cp.name=name;for(const k of cp.keyframes)delete k.uuid;animators[dst.get(name)]=cp;
      }
      if(!Object.keys(animators).length)err('Нет ключей для копирования');
      const c=JSON.parse(JSON.stringify(clip));for(const k of ['uuid','path','scope','selected','saved'])delete c[k];c.animators=animators;return c;
    }
    function gameLibrary(files){
      let base=null,combo=null;const animations=Object.create(null);
      for(const f of files){
        if(f.steps||f.dual_steps){if(combo)err('Выбери один weapon_combos JSON');combo=f;continue;}
        if(!f.animations||Array.isArray(f.animations))err('Нужен JSON из weapon_anims, не Bedrock animation.json');
        if(f.grip){if(base)err('Выбери один основной JSON оружия');base=f;}
        for(const [n,c] of Object.entries(f.animations)){if(Object.hasOwn(animations,n))err('Повторяющийся игровой клип '+n);animations[n]=c;}
      }
      if(!base)err('Выбери основной JSON оружия из weapon_anims (с grip)');
      return {base,animations,combo};
    }
    function defaultGameRequests(lib){
      if(lib.combo)return [...(lib.combo.steps||[]).map(s=>({right:s.anim})),...(lib.combo.dual_steps||[]).map(s=>({right:s.anim||'',left:s.off_anim||''}))].filter(s=>[s.right,s.left].some(Boolean));
      if(lib.animations.bb_author_dual_attack1&&lib.animations.bb_author_dual_attack1_off)return [1,2,3].map(i=>({right:'bb_author_attack'+i})).concat([1,2,3].map(i=>({right:'bb_author_dual_attack'+i,left:'bb_author_dual_attack'+i+'_off'})));
      if(lib.animations.axe_dual_chop&&lib.animations.axe_dual_chop_off)return [...(lib.animations.bb_author_attack1?[{right:'bb_author_attack1'},{right:'bb_author_attack2'},{right:'bb_author_attack3'}]:[]),{right:'axe_dual_chop'},{left:'axe_dual_chop_off'},{right:'axe_dual_slam',left:'axe_dual_slam_off'},{right:'axe_dual_slam_repeat',left:'axe_dual_slam_repeat_off'},{right:'axe_dual_slam_final',left:'axe_dual_slam_final_off'}];
      return Object.keys(lib.animations).filter(n=>!/(?:_third|_shoulder|_wrist|_edge|_raw|_off)$/.test(n)).map(n=>({right:n,left:lib.animations[n+'_off']?n+'_off':''}));
    }


    // Offline, calibrated Blockbench -> renderer library (numeric local keys).
    // This does not mutate the user's project. Missing F5 is calculated on a
    // temporary copy, using the same solver used by Generate F5.
    function bakeGame(project,original,name,fps=60){
      if(!/^bb_[a-z0-9_]+$/.test(name)||!Number.isInteger(fps)||fps<20||fps>240)err('Неверное имя или частота экспорта');
      let clip=JSON.parse(JSON.stringify(original));clip.loop='once';
      const auto=!existingF5(project,clip);
      if(auto){const plan=planF5(project,clip);if(plan.status!=='generated')err('Нет FP-ключей '+original.name);clip.animators={...clip.animators,...plan.animators};}
      const rr=rig(project,clip,true),out={},times=new Set([...rr.times].map(timeKey));
      for(let i=0;i/fps<rr.length;i++)times.add(timeKey(i/fps));
      for(const t of rr.jumps){if(t>0)times.add(timeKey(Math.max(0,t-.000001)));}
      const sorted=[...times].sort((a,b)=>a-b),active=rr.sides.filter(s=>s.FP.chain.some(g=>clip.animators[g.uuid]?.keyframes?.length));
      const round=x=>Number(x.toFixed(7)),gameKey=(t,r,p=[0,0,0])=>({t:timeKey(t),rotation:r.map(round),translation:p.map(round),ease:'linear'});
      const empty=()=>({length:rr.length,hold:0,return:0,smooth:false,rotation_mode:'slerp',third:{baked:true},keyframes:[]});
      let maxGripError=0,maxRotationError=0;
      for(const side of active){
        const m=side.sign,n=name+(m===1?'':'_off'),f=side.FP.b,t=side.F5.b,fpS=side.FP.chain[2],f5A=side.F5.chain[1],f5S=side.F5.chain[2];
        if(!side.F5.chain.some(g=>clip.animators[g.uuid]?.keyframes?.length))err('Нет F5 для '+side.side+' '+original.name);
        out[n]=empty();out[n+'_shoulder']=empty();out[n+'_wrist']=empty();
        out[n].third={source:n+'_shoulder',arm_space:true,wrist_source:n+'_wrist',wrist_xyz:true};
        const display=rr.meta.display['thirdperson_'+side.side+'hand'],dtp=rel(vec(display.rotation),m);
        const mount=signed(add(vec(rr.meta.fp_idle_offset),[0,0,0]),m);
        for(const time of sorted){
          const cache=new Map(),ps=rr.pose(fpS,time,cache),a=rr.pose(f5A,time,cache),s=rr.pose(f5S,time,cache);
          if(!near(a.o,vec(t.pivot)))err('F5 shoulder translation '+original.name);
          const local=mv(tr(a.R),sub(s.o,a.o)),expected=sub(vec(t.grip_position),vec(t.pivot));
          maxGripError=Math.max(maxGripError,Math.hypot(...sub(local,expected)));
          if(!near(local,expected))err('F5 hilt detached '+original.name);
          const relative=mm(ps.R,tr(f.item_rotation)),rotFP=xyzAngles(relative),pos=signed(add(sub(ps.o,vec(f.grip_position)),mount),m);
          const A=mm(mm(mm(F,a.R),F),rx(-Math.PI/10)),e=euler(A),angles=[e[0]+18,m*e[1],m*e[2]];
          const W=mm(mm(tr(mm(mm(F,A),C)),s.R),tr(dtp)),w=xyzAngles(W);
          const reconstructedFP=mm(rel([rotFP[0],m*rotFP[1],m*rotFP[2]],m),f.item_rotation);
          const reconstructedA=rot([angles[0]-18,m*angles[1],m*angles[2]]);
          const reconstructedS=mm(mm(mm(mm(F,reconstructedA),C),xyz(w)),dtp);
          const errM=(a,b)=>Math.max(...a.flatMap((row,i)=>row.map((v,j)=>Math.abs(v-b[i][j]))));
          maxRotationError=Math.max(maxRotationError,errM(reconstructedFP,ps.R),errM(reconstructedS,s.R));
          out[n].keyframes.push(gameKey(time,[rotFP[0],m*rotFP[1],m*rotFP[2]],pos));
          out[n+'_shoulder'].keyframes.push(gameKey(time,angles));
          out[n+'_wrist'].keyframes.push(gameKey(time,w));
        }
      }
      if(!active.length||maxRotationError>1e-5)err('Не прошёл обратный пересчёт '+original.name+' '+maxRotationError);
      return {animations:out,autoF5:auto,samples:sorted.length,maxGripError,maxRotationError};
    }

    return {planF5,planTiming,existingF5,rot,euler,importGame,gameLibrary,defaultGameRequests,copyClip,gameSample,bakeGame};
  })();


  // Persist metadata on the project object as well as the saved root, so
  // reloading the JS module cannot discard the active project's calibration.
  const LEGACY_PALE_META={"version":1,"archetype":"pale_linear_greatsword","euler_order":"ZYX","grip":[7.75,2,7.75],"bindings":{"F5_right":{"arm":"F5_right_arm","shoulder":"F5_right_shoulder","sword":"F5_right_sword","pivot":[5,22.016,0],"grip_position":[6.142522035376563,14.084276077054229,-2.9111084981368025],"item_rotation":[[0.20500387380049348,0.008484682072751243,-0.9787243850527664],[0.8706156592879014,-0.45848365120431533,0.17838473976507171],[-0.4472155917781533,-0.8886623384325927,-0.1013778216476955]],"scale":1},"F5_left":{"arm":"F5_left_arm","shoulder":"F5_left_shoulder","sword":"F5_left_sword","pivot":[-5,22.016,0],"grip_position":[-5.745904450627374,15.104952681989634,-2.6550968578244705],"item_rotation":[[-0.3475656014823668,0.1357897727129417,-0.9277711411187396],[0.8153179174645151,-0.44491041461704406,-0.37055555106706467],[-0.463092697121782,-0.8852207976498617,0.043923721160846675]],"scale":1},"FP_right":{"arm":"FP_right_arm","shoulder":"FP_right_shoulder","sword":"FP_right_sword","pivot":[8.347946042964802,3.6133735525641786,-0.6867986401000001],"grip_position":[8.347946042964802,-6.386626447435821,-4.6867986401],"item_rotation":[[0.2317740063965282,-0.21719398081517335,-0.9482128372135445],[0.9535038614305918,-0.14231181572846385,0.26566470096913225],[-0.19264266452645643,-0.9656987738429232,0.17411111969779913]],"scale":0.68},"FP_left":{"arm":"FP_left_arm","shoulder":"FP_left_shoulder","sword":"FP_left_sword","pivot":[-12.943829888154784,1.68972488604647,-4.865911123464654],"grip_position":[-12.943829888154784,-8.31027511395353,-8.865911123464654],"item_rotation":[[-0.11620764355680652,0.35935685778854976,0.9259365163656277],[0.4145779030895245,0.8647065037645311,-0.2835627348880909],[-0.9025635411697432,0.3509206621270127,-0.2494669177335384]],"scale":0.68}},"display":{"thirdperson_righthand":{"rotation":[-47.61,-78.16,-2.37],"translation":[0,2.75,-2.5]},"thirdperson_lefthand":{"rotation":[155.24,68.09,158.66],"translation":[-0.75,2.75,-1.5]},"firstperson_righthand":{"rotation":[-56.76,-71.48,43.14],"translation":[-1.62,2.2,1.13],"scale":[0.68,0.68,0.68]},"firstperson_lefthand":{"rotation":[131.34,-67.81,107.92],"translation":[2.38,4.2,2.13],"scale":[0.68,0.68,0.68]}},"camera_names":["TOI_FP_70","TOI_F5_front","TOI_F5_back"],"fp_mount":[0,-0.64,2.4],"fp_idle_offset":[0,0,-0.64],"source_sha256":{"src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel":"a336122c49a19367ba7ad0a30f97b8afbb13fae65695190facc3048eada51593","src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.json":"70b8cdc45b289c6cb9214e2b2e1aaefdd45eb5c4d6d112b200bdcc9cafeed089"},"calibration":"renderer-math only; live Minecraft comparison pending"};
  const LEGACY_PALE_GEOMETRY=[["F5_right_sword_cube_0",[17.39252204,24.58427608,-3.6611085],[18.39252204,26.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_1",[16.89252204,23.58427608,-3.6611085],[17.39252204,26.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_2",[18.39252204,25.08427608,-3.6611085],[18.89252204,26.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_3",[16.39252204,23.08427608,-3.6611085],[16.89252204,25.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_4",[15.89252204,22.58427608,-3.6611085],[16.39252204,25.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_5",[15.39252204,22.08427608,-3.6611085],[15.89252204,24.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_6",[17.39252204,24.08427608,-3.6611085],[17.89252204,24.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_7",[14.89252204,21.58427608,-3.6611085],[15.39252204,24.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_8",[14.39252204,21.08427608,-3.6611085],[14.89252204,23.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_9",[13.89252204,20.58427608,-3.6611085],[14.39252204,23.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_10",[13.39252204,20.08427608,-3.6611085],[13.89252204,22.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_11",[12.89252204,19.58427608,-3.6611085],[13.39252204,22.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_12",[12.39252204,19.08427608,-3.6611085],[12.89252204,21.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_13",[11.89252204,18.58427608,-3.6611085],[12.39252204,21.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_14",[11.39252204,18.08427608,-3.6611085],[11.89252204,20.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_15",[10.89252204,17.58427608,-3.6611085],[11.39252204,20.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_16",[10.39252204,17.08427608,-3.6611085],[10.89252204,19.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_17",[7.89252204,18.58427608,-3.6611085],[8.89252204,19.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_18",[9.89252204,16.58427608,-3.6611085],[10.39252204,19.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_19",[7.39252204,15.08427608,-3.6611085],[8.39252204,18.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_20",[9.39252204,15.08427608,-3.6611085],[9.89252204,18.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_21",[8.89252204,15.08427608,-3.6611085],[9.39252204,18.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_22",[8.39252204,15.08427608,-3.6611085],[8.89252204,17.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_23",[10.89252204,15.58427608,-3.6611085],[11.39252204,16.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_24",[9.89252204,15.08427608,-3.6611085],[10.89252204,16.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_25",[6.89252204,14.08427608,-3.6611085],[7.39252204,15.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_26",[6.39252204,13.58427608,-3.6611085],[6.89252204,15.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_27",[7.39252204,14.58427608,-3.6611085],[7.89252204,15.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_28",[5.89252204,13.08427608,-3.6611085],[6.39252204,14.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_29",[5.39252204,12.58427608,-3.6611085],[5.89252204,14.08427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_right_sword_cube_30",[4.89252204,12.58427608,-3.6611085],[5.39252204,13.58427608,-2.1611085],[7.39252204,15.08427608,-2.6611085],[0,0,0]],["F5_left_sword_cube_0",[5.50409555,25.60495268,-3.40509686],[6.50409555,27.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_1",[5.00409555,24.60495268,-3.40509686],[5.50409555,27.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_2",[6.50409555,26.10495268,-3.40509686],[7.00409555,27.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_3",[4.50409555,24.10495268,-3.40509686],[5.00409555,26.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_4",[4.00409555,23.60495268,-3.40509686],[4.50409555,26.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_5",[3.50409555,23.10495268,-3.40509686],[4.00409555,25.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_6",[5.50409555,25.10495268,-3.40509686],[6.00409555,25.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_7",[3.00409555,22.60495268,-3.40509686],[3.50409555,25.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_8",[2.50409555,22.10495268,-3.40509686],[3.00409555,24.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_9",[2.00409555,21.60495268,-3.40509686],[2.50409555,24.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_10",[1.50409555,21.10495268,-3.40509686],[2.00409555,23.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_11",[1.00409555,20.60495268,-3.40509686],[1.50409555,23.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_12",[0.50409555,20.10495268,-3.40509686],[1.00409555,22.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_13",[0.00409555,19.60495268,-3.40509686],[0.50409555,22.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_14",[-0.49590445,19.10495268,-3.40509686],[0.00409555,21.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_15",[-0.99590445,18.60495268,-3.40509686],[-0.49590445,21.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_16",[-1.49590445,18.10495268,-3.40509686],[-0.99590445,20.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_17",[-3.99590445,19.60495268,-3.40509686],[-2.99590445,20.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_18",[-1.99590445,17.60495268,-3.40509686],[-1.49590445,20.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_19",[-4.49590445,16.10495268,-3.40509686],[-3.49590445,19.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_20",[-2.49590445,16.10495268,-3.40509686],[-1.99590445,19.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_21",[-2.99590445,16.10495268,-3.40509686],[-2.49590445,19.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_22",[-3.49590445,16.10495268,-3.40509686],[-2.99590445,18.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_23",[-0.99590445,16.60495268,-3.40509686],[-0.49590445,17.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_24",[-1.99590445,16.10495268,-3.40509686],[-0.99590445,17.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_25",[-4.99590445,15.10495268,-3.40509686],[-4.49590445,16.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_26",[-5.49590445,14.60495268,-3.40509686],[-4.99590445,16.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_27",[-4.49590445,15.60495268,-3.40509686],[-3.99590445,16.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_28",[-5.99590445,14.10495268,-3.40509686],[-5.49590445,15.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_29",[-6.49590445,13.60495268,-3.40509686],[-5.99590445,15.10495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["F5_left_sword_cube_30",[-6.99590445,13.60495268,-3.40509686],[-6.49590445,14.60495268,-1.90509686],[-4.49590445,16.10495268,-2.40509686],[0,0,0]],["FP_right_sword_cube_0",[15.99794604,0.75337355,-5.19679864],[16.67794604,2.11337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_1",[15.65794604,0.07337355,-5.19679864],[15.99794604,1.77337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_2",[16.67794604,1.09337355,-5.19679864],[17.01794604,1.77337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_3",[15.31794604,-0.26662645,-5.19679864],[15.65794604,1.43337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_4",[14.97794604,-0.60662645,-5.19679864],[15.31794604,1.09337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_5",[14.63794604,-0.94662645,-5.19679864],[14.97794604,0.75337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_6",[15.99794604,0.41337355,-5.19679864],[16.33794604,0.75337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_7",[14.29794604,-1.28662645,-5.19679864],[14.63794604,0.41337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_8",[13.95794604,-1.62662645,-5.19679864],[14.29794604,0.07337355,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_9",[13.61794604,-1.96662645,-5.19679864],[13.95794604,-0.26662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_10",[13.27794604,-2.30662645,-5.19679864],[13.61794604,-0.60662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_11",[12.93794604,-2.64662645,-5.19679864],[13.27794604,-0.94662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_12",[12.59794604,-2.98662645,-5.19679864],[12.93794604,-1.28662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_13",[12.25794604,-3.32662645,-5.19679864],[12.59794604,-1.62662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_14",[11.91794604,-3.66662645,-5.19679864],[12.25794604,-1.96662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_15",[11.57794604,-4.00662645,-5.19679864],[11.91794604,-2.30662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_16",[11.23794604,-4.34662645,-5.19679864],[11.57794604,-2.64662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_17",[9.53794604,-3.32662645,-5.19679864],[10.21794604,-2.98662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_18",[10.89794604,-4.68662645,-5.19679864],[11.23794604,-2.98662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_19",[9.19794604,-5.70662645,-5.19679864],[9.87794604,-3.32662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_20",[10.55794604,-5.70662645,-5.19679864],[10.89794604,-3.32662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_21",[10.21794604,-5.70662645,-5.19679864],[10.55794604,-3.66662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_22",[9.87794604,-5.70662645,-5.19679864],[10.21794604,-4.00662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_23",[11.57794604,-5.36662645,-5.19679864],[11.91794604,-4.68662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_24",[10.89794604,-5.70662645,-5.19679864],[11.57794604,-5.02662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_25",[8.85794604,-6.38662645,-5.19679864],[9.19794604,-5.36662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_26",[8.51794604,-6.72662645,-5.19679864],[8.85794604,-5.70662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_27",[9.19794604,-6.04662645,-5.19679864],[9.53794604,-5.70662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_28",[8.17794604,-7.06662645,-5.19679864],[8.51794604,-6.04662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_29",[7.83794604,-7.40662645,-5.19679864],[8.17794604,-6.38662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_right_sword_cube_30",[7.49794604,-7.40662645,-5.19679864],[7.83794604,-6.72662645,-4.17679864],[9.19794604,-5.70662645,-4.51679864],[0,0,0]],["FP_left_sword_cube_0",[-5.29382989,-1.17027511,-9.37591112],[-4.61382989,0.18972489,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_1",[-5.63382989,-1.85027511,-9.37591112],[-5.29382989,-0.15027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_2",[-4.61382989,-0.83027511,-9.37591112],[-4.27382989,-0.15027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_3",[-5.97382989,-2.19027511,-9.37591112],[-5.63382989,-0.49027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_4",[-6.31382989,-2.53027511,-9.37591112],[-5.97382989,-0.83027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_5",[-6.65382989,-2.87027511,-9.37591112],[-6.31382989,-1.17027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_6",[-5.29382989,-1.51027511,-9.37591112],[-4.95382989,-1.17027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_7",[-6.99382989,-3.21027511,-9.37591112],[-6.65382989,-1.51027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_8",[-7.33382989,-3.55027511,-9.37591112],[-6.99382989,-1.85027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_9",[-7.67382989,-3.89027511,-9.37591112],[-7.33382989,-2.19027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_10",[-8.01382989,-4.23027511,-9.37591112],[-7.67382989,-2.53027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_11",[-8.35382989,-4.57027511,-9.37591112],[-8.01382989,-2.87027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_12",[-8.69382989,-4.91027511,-9.37591112],[-8.35382989,-3.21027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_13",[-9.03382989,-5.25027511,-9.37591112],[-8.69382989,-3.55027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_14",[-9.37382989,-5.59027511,-9.37591112],[-9.03382989,-3.89027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_15",[-9.71382989,-5.93027511,-9.37591112],[-9.37382989,-4.23027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_16",[-10.05382989,-6.27027511,-9.37591112],[-9.71382989,-4.57027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_17",[-11.75382989,-5.25027511,-9.37591112],[-11.07382989,-4.91027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_18",[-10.39382989,-6.61027511,-9.37591112],[-10.05382989,-4.91027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_19",[-12.09382989,-7.63027511,-9.37591112],[-11.41382989,-5.25027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_20",[-10.73382989,-7.63027511,-9.37591112],[-10.39382989,-5.25027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_21",[-11.07382989,-7.63027511,-9.37591112],[-10.73382989,-5.59027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_22",[-11.41382989,-7.63027511,-9.37591112],[-11.07382989,-5.93027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_23",[-9.71382989,-7.29027511,-9.37591112],[-9.37382989,-6.61027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_24",[-10.39382989,-7.63027511,-9.37591112],[-9.71382989,-6.95027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_25",[-12.43382989,-8.31027511,-9.37591112],[-12.09382989,-7.29027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_26",[-12.77382989,-8.65027511,-9.37591112],[-12.43382989,-7.63027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_27",[-12.09382989,-7.97027511,-9.37591112],[-11.75382989,-7.63027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_28",[-13.11382989,-8.99027511,-9.37591112],[-12.77382989,-7.97027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_29",[-13.45382989,-9.33027511,-9.37591112],[-13.11382989,-8.31027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]],["FP_left_sword_cube_30",[-13.79382989,-9.33027511,-9.37591112],[-13.45382989,-8.65027511,-8.35591112],[-12.09382989,-7.63027511,-8.69591112],[0,0,0]]];
  const copyMeta=m=>JSON.parse(JSON.stringify(m));
  const nearVec=(a,b,tol=1e-4)=>Array.isArray(a)&&Array.isArray(b)&&a.length===b.length&&a.every((x,i)=>typeof x==='number'&&Number.isFinite(x)&&Math.abs(x-b[i])<=tol);
  function compatibleMetadata(project,meta){
    if(!meta||![1,2].includes(meta.version)||meta.euler_order!=='ZYX'||!nearVec(meta.fp_mount,meta.fp_mount)||meta.fp_mount.length!==3)return false;
    const groups=new Map((project.groups||[]).map(g=>[g.name,g]));
    for(const view of ['FP','F5'])for(const side of ['right','left']){
      const b=meta.bindings?.[view+'_'+side];
      if(!b||!Number.isFinite(b.scale)||b.scale<=0)return false;
      for(const k of ['shoulder','arm','sword']){
        const g=groups.get(b[k]);if(!g||!nearVec(g.origin,k==='sword'?b.grip_position:b.pivot))return false;
        const r=f5math.rot(g.rotation||[0,0,0]);
        const expected=k==='sword'?b.item_rotation:[[1,0,0],[0,1,0],[0,0,1]];
        if(!expected||!r.every((v,i)=>nearVec(v,expected[i])))return false;
      }
    }
    return true;
  }
  function sameWeaponGeometry(project,rows){
    const cubes=(project.elements||[]).filter(e=>/^(FP|F5)_(right|left)_sword_cube_\d+$/.test(e.name));
    const map=new Map(cubes.map(e=>[e.name,e]));
    return cubes.length===rows.length&&map.size===rows.length&&rows.every(([name,from,to,origin,rotation])=>{
      const e=map.get(name);
      return e&&nearVec(e.from,from)&&nearVec(e.to,to)&&nearVec(e.origin,origin)&&nearVec(e.rotation||[0,0,0],rotation);
    });
  }
  function recoverMetadata(project,saved){
    if(compatibleMetadata(project,project.toi_workspace))return copyMeta(project.toi_workspace);
    // A disk copy may have older animation keys: read only metadata and
    // compare calibration and weapon geometry, never copy any animation.
    if(saved&&compatibleMetadata(project,saved.toi_workspace)){
      const rows=(saved.elements||[]).filter(e=>/^(FP|F5)_(right|left)_sword_cube_\d+$/.test(e.name))
        .map(e=>[e.name,e.from,e.to,e.origin,e.rotation||[0,0,0]]);
      if(rows.length&&sameWeaponGeometry(project,rows))return copyMeta(saved.toi_workspace);
    }
    // V1 Pale workspace (and identical calibrated copies) can be restored
    // independently of project name, uuid, animation count and filenames.
    if(compatibleMetadata(project,LEGACY_PALE_META)&&sameWeaponGeometry(project,LEGACY_PALE_GEOMETRY))return copyMeta(LEGACY_PALE_META);
    return null;
  }

  if(typeof BBPlugin==='undefined'&&typeof module!=='undefined'){module.exports=Object.assign(buildWeaponProject,f5math,{recoverMetadata,compatibleMetadata,normalizePistolSource});return;}

  function attachCalibration(model){
    if(!Project||!model.groups||!model.elements)return model;
    const current=Project.toiWorkspaceCalibration||projectMetadata.get(Project)||model.toi_workspace;
    let meta=null;
    if(current&&compatibleMetadata(model,current))meta=copyMeta(current);
    if(!meta){
      let disk=null;
      try{if(typeof require==='function'&&Project.save_path){const fs=require('fs');const raw=fs.readFileSync(Project.save_path,'utf8').replace(/^\uFEFF/,'');disk=JSON.parse(raw);}}catch(e){/* plain JSON fallback below */}
      meta=recoverMetadata(model,disk);
    }
    if(meta){Project.toiWorkspaceCalibration=meta;projectMetadata.set(Project,meta);model.toi_workspace=copyMeta(meta);}
    return model;
  }
  function compiledProject(){const raw=Codecs.project.compile({raw:true});return attachCalibration(typeof raw==='string'?JSON.parse(raw):raw);}
  const failDialog=e=>Blockbench.showMessageBox({title:'Tears of Ink',message:String(e.message||e)});
  function parseVector(text){const v=String(text).split(/[,; ]+/).filter(Boolean).map(Number);if(v.length!==3||v.some(x=>!Number.isFinite(x)))throw Error('Введите три числа X, Y, Z');return v;}
  function makeAtlas(source){
    return new Promise((resolve,reject)=>{
      try{
        const ids=[...new Set(source.elements.filter(e=>e.export!==false).flatMap(e=>Object.values(e.faces||{}).filter(f=>f.texture!==null&&f.texture!==false).map(f=>f.texture)))];
        const tex=source.textures?.[ids[0]];
        if(!tex||!/^data:image\//.test(tex.source||'')||ids.some(i=>source.textures?.[i]?.source!==tex.source))throw Error('Нужна одна используемая встроенная текстура (одинаковые дубликаты допустимы)');
        const img=new Image();img.onerror=()=>reject(Error('Не удалось загрузить текстуру'));
        img.onload=()=>{try{const w=img.naturalWidth,h=img.naturalHeight;if(w<1||h<1||w>2048||h>2048)throw Error('Неподдерживаемый размер текстуры');
          const canvas=document.createElement('canvas');canvas.width=w+32;canvas.height=Math.max(h,16);const ctx=canvas.getContext('2d');ctx.imageSmoothingEnabled=false;ctx.drawImage(img,0,0);
          ['#be9b78','#375a91','#414655','#be8255','#9b966e'].forEach((color,i)=>{ctx.fillStyle=color;ctx.fillRect(w+(i%4)*8,Math.floor(i/4)*8,8,8);});
          resolve({width:canvas.width,height:canvas.height,itemWidth:w,itemHeight:h,textureIds:ids,source:canvas.toDataURL('image/png')});
        }catch(e){reject(e);}};img.src=tex.source;
      }catch(e){reject(e);}
    });
  }
  function readConfig(file,arch){
    try{if(!file.path||typeof require!=='function')return null;const fs=require('fs'),path=require('path');let dir=path.dirname(file.path);
      for(let i=0;i<14;i++){const f=path.join(dir,'src','main','resources','assets','tears-of-ink','weapon_anims',arch+'.json');if(fs.existsSync(f))return JSON.parse(fs.readFileSync(f,'utf8'));const parent=path.dirname(dir);if(parent===dir)break;dir=parent;}
    }catch(e){console.warn('TOI config lookup:',e);}return null;
  }
  function saveSeparate(model,name){Blockbench.export({type:'Blockbench Project',extensions:['bbmodel'],name,content:JSON.stringify(model,null,2),savetype:'text'});}
  function newWeapon(){
    if(!isWorkspace())return;
    try{const template=compiledProject();
      Blockbench.import({extensions:['bbmodel'],type:'Java weapon project',multiple:false,readtype:'text'},files=>{
        try{if(!files?.length)return;const file=files[0],source=JSON.parse(file.content);
          const aliases={pale_linear_great:'pale_linear_greatsword',blood_talvar:'talwar',blood_dussak:'dussak',spire_torturer_greatsword:'executioner',spire_roaring_axe:'axe',spire_rapid_knife:'dagger',spire_shadow_scimitar:'scimitar'};
          const base=(file.name||source.name||'weapon').replace(/\.bbmodel$/i,'').toLowerCase(),arch=aliases[base]||base.replace(/[^a-z0-9_]/g,'_'),config=readConfig(file,arch);
          const dialog=new Dialog('toi_weapon_create',{title:'Tears of Ink — Новое оружие',form:{
            archetype:{label:'ID анимаций',type:'text',value:arch},
            grip:{label:'Хват X, Y, Z (пиксели исходной модели)',type:'text',value:config?.grip?.join(', ')||(arch==='holy_pistol'?'10, 6.5, 11':'')},
            mount:{label:'FP offset X, Y, Z',type:'text',value:(config?.firstperson_offset||[0,0,0]).join(', ')},
            idle:{label:'FP idle X, Y, Z',type:'text',value:(config?.firstperson_idle_offset||[0,0,0]).join(', ')}
          },onConfirm(form){dialog.hide();(async()=>{
            const cfg={archetype:form.archetype.trim(),grip:parseVector(form.grip),fp_mount:parseVector(form.mount),fp_idle_offset:parseVector(form.idle),display:source.display||{},makeId:()=>guid()};
            const prepared=cfg.archetype==='holy_pistol'?normalizePistolSource(source):source;
            cfg.display=prepared.display||{};
            const model=buildWeaponProject(template,prepared,cfg,await makeAtlas(prepared));saveSeparate(model,model.name);
            Blockbench.showQuickMessage('Сохрани и открой новый .bbmodel. Исходник не изменён.');
          })().catch(failDialog);}});dialog.show();
        }catch(e){failDialog(e);}
      });
    }catch(e){failDialog(e);}
  }
  function saveForGame(){if(!isWorkspace())return;try{const model=compiledProject();if(!model.toi_workspace)throw Error('Переоткрой сцену с включённым помощником: нет метаданных рига');saveSeparate(model,model.toi_workspace.archetype+'_animation');}catch(e){failDialog(e);}}


  // Blockbench 5.2.2 Animation.extend only replaces supplied animators.
  // Undo snapshots require the previously empty target containers to exist.
  function applyF5Plans(plans,label){
    const effective=plans.filter(p=>Object.keys(p.plan.animators).length);
    if(!effective.length)return;
    for(const {animation,plan} of effective)for(const uid of Object.keys(plan.animators)){
      const group=Group.all.find(g=>g.uuid===uid);
      if(!group)throw Error('Кость исчезла: '+uid);
      animation.getBoneAnimator(group);
    }
    Undo.initEdit({animations:effective.map(p=>p.animation)});
    try{
      for(const {animation,plan} of effective){
        const animators={};
        for(const [uid,a] of Object.entries(plan.animators))animators[uid]={...a,keyframes:(a.keyframes||[]).map(k=>({...k,uuid:k.uuid||guid()}))};
        animation.extend({animators});
      }
      Undo.finishEdit(label);
    }catch(e){Undo.cancelEdit(true);throw e;}
    if(typeof Timeline!=='undefined'&&Array.isArray(Timeline.selected))Timeline.selected.splice(0);
    if(Modes.animate){
      const selected=Animation.selected;
      if(selected){
        const p=effective.find(p=>p.animation===selected);
        const pair=p?.plan.sides?p.plan.sides.includes('left'):both;
        setup('F5',pair,'front');
        for(const [uid,a] of Object.entries(selected.animators))if(Group.all.some(g=>g.uuid===uid&&g.name.startsWith('F5_'))&&a.keyframes.length)a.addToTimeline(true);
      }
      Animator.preview();
    }
  }
  function generateF5(all=false){
    try{
      if(!isWorkspace()||!Modes.animate)throw Error('Открой TOI-сцену и вкладку Animate');
      const project=compiledProject(),selected=Animation.selected;
      if(!all&&!selected)throw Error('Выбери анимацию слева');
      const owner=Project;
      const run=(force=false)=>{
        try{
          if(Project!==owner||(!all&&Animation.selected!==selected))throw Error('Сцена или анимация изменилась — повтори действие');
          const current=compiledProject(),animations=all?Animator.animations:[selected],plans=[],messages=[];
          for(const animation of animations){
            try{
              const plan=f5math.planF5(current,animation.getUndoCopy(),force);
              if(plan.status==='generated')plans.push({animation,plan});
              else messages.push(animation.name+': '+(plan.status==='protected'?'F5 уже есть, пропущено':'нет ключей FP'));
            }catch(e){if(!all)throw e;messages.push(animation.name+': '+e.message);}
          }
          applyF5Plans(plans,all?'TOI: Generate all F5':'TOI: Generate F5');
          const msg='F5 создано: '+plans.length+(messages.length?'\n\nПропущено: '+messages.length+'\n'+messages.slice(0,16).join('\n')+(messages.length>16?'\n…':''):'');
          if(all||messages.length)Blockbench.showMessageBox({title:'Tears of Ink — F5',message:msg});
          else Blockbench.showQuickMessage(msg+' · Ctrl+Z — отмена');
        }catch(e){failDialog(e);}
      };
      if(!all&&f5math.existingF5(project,selected.getUndoCopy())){
        Blockbench.showMessageBox({title:'Tears of Ink — F5 уже существует',message:'Заменить существующие ключи F5 на рассчитанные для рук с FP-ключами? Авторские FP и остальные анимации останутся без изменений. Действие можно отменить Ctrl+Z.',buttons:['Заменить F5','Отмена'],confirm:0,cancel:1},i=>{if(i===0)run(true);});
      }else run();
    }catch(e){failDialog(e);}
  }
  function syncF5Timing(){
    try{
      if(!isWorkspace()||!Modes.animate||!Animation.selected)throw Error('Выбери анимацию во вкладке Animate');
      const animation=Animation.selected,plan=f5math.planTiming(compiledProject(),animation.getUndoCopy());
      applyF5Plans([{animation,plan}],'TOI: Sync F5 timing');
      Blockbench.showMessageBox({title:'Tears of Ink — Sync timing',message:'Добавлено F5-ключей на временах FP: '+plan.inserted+'. Существующие ключи не перемещены и не изменены.'+(plan.skipped?.length?'\nСложные кривые оставлены без изменений: '+plan.skipped.join(', '):'')});
    }catch(e){failDialog(e);}
  }


  let clipClipboard=null;
  function addClips(blueprints,label){
    if(!blueprints.length)throw Error('Нет анимаций для импорта');
    const used=new Set(Animator.animations.map(a=>a.name));
    const prepared=blueprints.map(c=>{
      let name=c.name||'animation',n=2;while(used.has(name))name=(c.name||'animation')+'_copy'+n++;
      used.add(name);const animators={};for(const [id,a] of Object.entries(c.animators))animators[id]={...a,keyframes:(a.keyframes||[]).map(k=>({...k,uuid:guid()}))};
      const data={...c,name,animators};delete data.uuid;delete data.selected;return data;
    });
    const added=[];Undo.initEdit({animations:[]});
    try{for(const data of prepared){const animation=new Animation(data).add(false);added.push(animation);}
      added[0].select();Undo.finishEdit(label,{animations:added});
    }catch(e){for(const a of added)a.remove(false);Undo.cancelEdit(true);throw e;}
    Animator.preview();Blockbench.showQuickMessage('Добавлено анимаций: '+added.length+' · Ctrl+Z — отмена');
  }
  function requireAnimate(){if(!isWorkspace()||!Modes.animate)throw Error('Открой TOI-сцену во вкладке Animate');}
  function copyTOIClip(){
    try{requireAnimate();if(!Animation.selected)throw Error('Выбери анимацию слева');
      const p=compiledProject();clipClipboard={groups:JSON.parse(JSON.stringify(p.groups)),meta:p.toi_workspace,clip:Animation.selected.getUndoCopy()};
      Blockbench.showQuickMessage('Скопировано: '+Animation.selected.name+' · открой другую сцену и Paste clip');
    }catch(e){failDialog(e);}
  }
  function pasteTOIClip(){
    try{requireAnimate();if(!clipClipboard)throw Error('Сначала Copy clip в исходной сцене');
      const p=compiledProject();if(!p.toi_workspace||!clipClipboard.meta)throw Error('Нужна калиброванная TOI-сцена');
      addClips([f5math.copyClip(clipClipboard,clipClipboard.clip,p)],'TOI: Paste clip');
      if(clipClipboard.meta.archetype!==p.toi_workspace.archetype)Blockbench.showQuickMessage('Перенесены локальные ключи; проверь посадку на новом оружии.');
    }catch(e){failDialog(e);}
  }
  function importFromGame(){
    try{requireAnimate();const owner=Project;
      Blockbench.import({extensions:['json'],type:'TOI weapon_anims / weapon_combos JSON',multiple:true,readtype:'text'},files=>{
        try{if(!files?.length)return;if(Project!==owner)throw Error('Сцена изменилась — повтори импорт');
          const lib=f5math.gameLibrary(files.map(f=>JSON.parse(f.content))),preset=f5math.defaultGameRequests(lib);
          const dlg=new Dialog('toi_import_game',{title:'Tears of Ink — Import from game',form:{
            clips:{label:'Клипы: один на строку; правый + левый (пустой правый допустим)',type:'textarea',value:preset.map(s=>(s.right||'')+(s.left?' + '+s.left:'')).join('\n')},
            fps:{label:'Ключей в секунду (20–60)',type:'number',value:30,min:20,max:60}
          },onConfirm(form){dlg.hide();try{
            if(Project!==owner)throw Error('Сцена изменилась — повтори импорт');
            const requests=String(form.clips).split(/\n|,/).map(s=>s.trim()).filter(Boolean).map(s=>{const parts=s.split('+').map(x=>x.trim());if(parts.length>2)throw Error('Две руки максимум');return {right:parts[0],left:parts[1]||''}});
            const fps=Math.max(20,Math.min(60,Math.round(Number(form.fps)||30)));
            addClips(f5math.importGame(compiledProject(),lib,requests,fps),'TOI: Import game clips');
          }catch(e){failDialog(e);}}});dlg.show();
        }catch(e){failDialog(e);}
      });
    }catch(e){failDialog(e);}
  }

  let mode = 'FP', both = true, angle = 'front';
  const isWorkspace = () => typeof Group !== 'undefined' && Group.all.some(g => g.name === 'FP_RIG') && Group.all.some(g => g.name === 'F5_RIG');
  function setup(view = mode, pair = both, facing = angle) {
    if (!isWorkspace()) return;
    if (typeof CameraElement === 'undefined') {
      Blockbench.showMessageBox({title:'Tears of Ink', message:'Install Cameras by JannisX11 first (File → Plugins), then reopen this workspace.'});
      return;
    }
    mode = view; both = pair; angle = facing;
    Group.all.forEach(g => { if (g.selected && !g.name.startsWith(mode + '_')) g.unselect();
      if (g.name === (mode === 'FP' ? 'F5_RIG' : 'FP_RIG')) g.isOpen = false;
      if (g.name === mode + '_RIG') g.isOpen = true; });
    for (const obj of [...Group.all, ...Outliner.elements]) {
      if (/^(FP|F5)_/.test(obj.name)) {
        obj.visibility = obj.name.startsWith(mode + '_') && (both || !obj.name.startsWith(mode + '_left_sword'));
        if (obj.mesh) obj.mesh.visible = obj.visibility;
      }
    }
    if (typeof Timeline !== 'undefined') {
      const visible = Timeline.animators.filter(a => a.group && a.group.name.startsWith(mode + '_'));
      Timeline.animators.splice(0, Timeline.animators.length, ...visible);
      if (Timeline.selected_animator && !visible.includes(Timeline.selected_animator)) Timeline.selected_animator = null;
    }
    Canvas.updateVisibility();
    if (Preview.split_screen.mode !== 'double_vertical') Preview.split_screen.setMode('double_vertical');
    // Blockbench's double_vertical layout places preview_1 LEFT, preview_0 RIGHT.
    const pinned = Preview.split_screen.previews[0], editor = Preview.split_screen.previews[1];
    CameraElement.all.forEach(c => c.linked_preview = '');
    const camera = CameraElement.all.find(c => c.name === (mode === 'FP' ? 'TOI_FP_70' : 'TOI_F5_' + angle));
    if (!camera) { Blockbench.showQuickMessage('TOI camera missing: reopen the original workspace with Cameras installed.'); return; }
    camera.linked_preview = pinned.id;
    pinned.aspect_ratio = 16 / 9;
    camera.updatePreviewCamera();
    // Cameras adds 0.1% padding by design; this fixed frame uses the exact chosen FOV.
    pinned.setFOV(camera.fov); pinned.resize(); pinned.controls.unlinked = true;
    editor.controls.unlinked = false; editor.setProjectionMode(false);
    editor.camera.position.fromArray(mode === 'FP' ? [25, 14, 30] : [45, 30, 55]);
    editor.controls.target.fromArray(mode === 'FP' ? [0,-5,-5] : [0,16,0]); editor.controls.update();
    if (frameObserver) frameObserver.disconnect();
    if (frame) frame.remove();
    frame = document.createElement('div');
    frame.style.cssText = 'position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);pointer-events:none;border:1px solid #93b8c6;z-index:5;color:white;font:11px sans-serif';
    frame.innerHTML = '<div style="position:absolute;bottom:100%;left:0;background:#151d28cc;padding:3px">TOI '+mode+' · '+camera.fov+'° · 16:9 · renderer math (Minecraft comparison pending)</div>'
      + (mode === 'FP' ? '<div style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%)">+</div><div style="position:absolute;top:92.6%;width:100%;border-top:1px dashed #ffffff55"></div>' : '');
    pinned.node.append(frame);
    const fitFrame = () => { frame.style.width = pinned.canvas.clientWidth + 'px'; frame.style.height = pinned.canvas.clientHeight + 'px'; };
    frameObserver = new ResizeObserver(fitFrame); frameObserver.observe(pinned.canvas); fitFrame();
  }
  function clearBar(){if(bar)bar.remove();bar=null;if(frame)frame.remove();frame=null;if(frameObserver)frameObserver.disconnect();frameObserver=null;}
  function installBar() {
    clearBar();
    if (!isWorkspace()) return;
    bar = document.createElement('div');
    bar.style.cssText = 'position:absolute;top:5px;left:5px;z-index:30;display:flex;flex-wrap:wrap;max-width:calc(100% - 10px);gap:3px;background:#20232dcc;padding:4px;border-radius:4px';
    for (const [label,v,p,f] of [['FP 1','FP',false,'front'],['FP 2','FP',true,'front'],['F5 1 front','F5',false,'front'],['F5 2 front','F5',true,'front'],['F5 back','F5',true,'back']]) {
      let b=document.createElement('button');b.textContent=label;b.style.cssText='font-size:11px;padding:4px;min-width:0;width:auto';b.onclick=()=>setup(v,p,f);bar.append(b);
    }
    for(const [label,click] of [['New weapon',newWeapon],['Save for game',saveForGame],['Generate F5',()=>generateF5(false)],['Generate all F5',()=>generateF5(true)],['Sync timing',syncF5Timing],['Import from game',importFromGame],['Copy clip',copyTOIClip],['Paste clip',pasteTOIClip]]){const b=document.createElement('button');b.textContent=label;b.style.cssText='font-size:11px;padding:4px;min-width:0;width:auto';b.onclick=click;bar.append(b);}
    Interface.preview.append(bar); setup('FP',true,'front');
  }
  BBPlugin.register('toi_workspace', {
    title:'Tears of Ink Weapon Workspace',author:'Tears of Ink',icon:'sports_martial_arts',version:'2.2.1',min_version:'5.0.0',variant:'both',
    description:'Separate FP/F5 rigs, automatic F5 from FP, safe batch generation and 16:9 preview.',
    onload() {
      let parsed=e=>{if(e.model.toi_workspace&&Project){const meta=copyMeta(e.model.toi_workspace);Project.toiWorkspaceCalibration=meta;projectMetadata.set(Project,meta);}setTimeout(installBar,100);};
      let compiled=e=>{if(isWorkspace()&&Project&&!e.options?.collection_only)attachCalibration(e.model);};
      Codecs.project.on('parsed',parsed);Codecs.project.on('compile',compiled);
      const selected=()=>setTimeout(installBar,100), subscription=Blockbench.on('select_project',selected);
      callbacks=[()=>Codecs.project.removeListener('parsed',parsed),()=>Codecs.project.removeListener('compile',compiled),()=>{if(subscription?.delete)subscription.delete();else Blockbench.removeListener?.('select_project',selected);}];
      let action=new Action('toi_workspace_reset',{name:'Tears of Ink: restore split workspace',icon:'view_column',click:installBar});
      MenuBar.addAction(action,'view');actions.push(action);
      for(const [id,name,click] of [['toi_weapon_new','Tears of Ink: new weapon',newWeapon],['toi_weapon_save','Tears of Ink: save animation project for game',saveForGame],['toi_generate_f5','Tears of Ink: Generate F5',()=>generateF5(false)],['toi_generate_all_f5','Tears of Ink: Generate all F5',()=>generateF5(true)],['toi_sync_f5_timing','Tears of Ink: Sync timing',syncF5Timing],['toi_import_game','Tears of Ink: Import from game',importFromGame],['toi_copy_clip','Tears of Ink: Copy clip',copyTOIClip],['toi_paste_clip','Tears of Ink: Paste clip',pasteTOIClip]]){const a=new Action(id,{name,icon:'sports_martial_arts',condition:isWorkspace,click});MenuBar.addAction(a,'view');actions.push(a);}
      if(isWorkspace()) setTimeout(installBar,100);
    },
    onunload() { callbacks.forEach(f=>f());actions.forEach(a=>a.delete());clearBar(); }
  });
})();
