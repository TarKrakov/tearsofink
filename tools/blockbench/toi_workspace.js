/* Tears of Ink artist workspace. Editor-only: requires Cameras by JannisX11. */
(function () {
  const projectMetadata=new WeakMap();
  let bar, frame, actions = [], callbacks = [], frameObserver;
  function buildWeaponProject(template,source,cfg,atlas) {
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
 if(!raw.length||raw.some(e=>e.type!=='cube'||e.box_uv||e.rescale||(e.inflate||0)!==0||(e.rotation||[]).filter(n=>Math.abs(n)>1e-8).length>1))fail('Use Java cubes with face UV and single-axis rotations');
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
  if(typeof BBPlugin==='undefined'&&typeof module!=='undefined'){module.exports=buildWeaponProject;return;}

  function compiledProject(){const raw=Codecs.project.compile({raw:true});return typeof raw==='string'?JSON.parse(raw):raw;}
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
            grip:{label:'Хват X, Y, Z (пиксели исходной модели)',type:'text',value:config?.grip?.join(', ')||''},
            mount:{label:'FP offset X, Y, Z',type:'text',value:(config?.firstperson_offset||[0,0,0]).join(', ')},
            idle:{label:'FP idle X, Y, Z',type:'text',value:(config?.firstperson_idle_offset||[0,0,0]).join(', ')}
          },onConfirm(form){dialog.hide();(async()=>{
            const cfg={archetype:form.archetype.trim(),grip:parseVector(form.grip),fp_mount:parseVector(form.mount),fp_idle_offset:parseVector(form.idle),display:source.display||{},makeId:()=>guid()};
            const model=buildWeaponProject(template,source,cfg,await makeAtlas(source));saveSeparate(model,model.name);
            Blockbench.showQuickMessage('Сохрани и открой новый .bbmodel. Исходник не изменён.');
          })().catch(failDialog);}});dialog.show();
        }catch(e){failDialog(e);}
      });
    }catch(e){failDialog(e);}
  }
  function saveForGame(){if(!isWorkspace())return;try{const model=compiledProject();if(!model.toi_workspace)throw Error('Переоткрой сцену с включённым помощником: нет метаданных рига');saveSeparate(model,model.toi_workspace.archetype+'_animation');}catch(e){failDialog(e);}}

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
    bar.style.cssText = 'position:absolute;top:5px;left:5px;z-index:30;display:flex;gap:3px;background:#20232dcc;padding:4px;border-radius:4px';
    for (const [label,v,p,f] of [['FP 1','FP',false,'front'],['FP 2','FP',true,'front'],['F5 1 front','F5',false,'front'],['F5 2 front','F5',true,'front'],['F5 back','F5',true,'back']]) {
      let b=document.createElement('button');b.textContent=label;b.style.cssText='font-size:11px;padding:4px;min-width:0;width:auto';b.onclick=()=>setup(v,p,f);bar.append(b);
    }
    for(const [label,click] of [['New weapon',newWeapon],['Save for game',saveForGame]]){const b=document.createElement('button');b.textContent=label;b.style.cssText='font-size:11px;padding:4px;min-width:0;width:auto';b.onclick=click;bar.append(b);}
    Interface.preview.append(bar); setup('FP',true,'front');
  }
  BBPlugin.register('toi_workspace', {
    title:'Tears of Ink Weapon Workspace',author:'Tears of Ink',icon:'sports_martial_arts',version:'2.0.0',min_version:'5.0.0',variant:'both',
    description:'Separate FP/F5 rigs; free editor + pinned Cameras preview, with 16:9 frame.',
    onload() {
      let parsed=e=>{if(e.model.toi_workspace&&Project)projectMetadata.set(Project,e.model.toi_workspace);setTimeout(installBar,100);};
      let compiled=e=>{if(isWorkspace()&&Project&&projectMetadata.has(Project))e.model.toi_workspace=projectMetadata.get(Project);};
      Codecs.project.on('parsed',parsed);Codecs.project.on('compile',compiled);
      const selected=()=>setTimeout(installBar,100), subscription=Blockbench.on('select_project',selected);
      callbacks=[()=>Codecs.project.removeListener('parsed',parsed),()=>Codecs.project.removeListener('compile',compiled),()=>{if(subscription?.delete)subscription.delete();else Blockbench.removeListener?.('select_project',selected);}];
      let action=new Action('toi_workspace_reset',{name:'Tears of Ink: restore split workspace',icon:'view_column',click:installBar});
      MenuBar.addAction(action,'view');actions.push(action);
      for(const [id,name,click] of [['toi_weapon_new','Tears of Ink: new weapon',newWeapon],['toi_weapon_save','Tears of Ink: save animation project for game',saveForGame]]){const a=new Action(id,{name,icon:'sports_martial_arts',condition:isWorkspace,click});MenuBar.addAction(a,'view');actions.push(a);}
      if(isWorkspace()) setTimeout(installBar,100);
    },
    onunload() { callbacks.forEach(f=>f());actions.forEach(a=>a.delete());clearBar(); }
  });
})();
