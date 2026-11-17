/* Tears of Ink artist workspace. Editor-only: requires Cameras by JannisX11. */
(function () {
  let metadata, bar, frame, actions = [], callbacks = [], frameObserver;
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
  function installBar() {
    if (!isWorkspace()) return;
    if (bar) bar.remove();
    bar = document.createElement('div');
    bar.style.cssText = 'position:absolute;top:5px;left:5px;z-index:30;display:flex;gap:3px;background:#20232dcc;padding:4px;border-radius:4px';
    for (const [label,v,p,f] of [['FP 1','FP',false,'front'],['FP 2','FP',true,'front'],['F5 1 front','F5',false,'front'],['F5 2 front','F5',true,'front'],['F5 back','F5',true,'back']]) {
      let b=document.createElement('button');b.textContent=label;b.style.cssText='font-size:11px;padding:4px;min-width:0;width:auto';b.onclick=()=>setup(v,p,f);bar.append(b);
    }
    Interface.preview.append(bar); setup('FP',true,'front');
  }
  BBPlugin.register('toi_workspace', {
    title:'Tears of Ink Weapon Workspace',author:'Tears of Ink',icon:'sports_martial_arts',version:'1.0.0',min_version:'5.0.0',variant:'both',
    description:'Separate FP/F5 rigs; free editor + pinned Cameras preview, with 16:9 frame.',
    onload() {
      let parsed = e => { metadata=e.model.toi_workspace; if(metadata) setTimeout(installBar,100); };
      let compiled = e => { if(isWorkspace() && metadata) e.model.toi_workspace=metadata; };
      Codecs.project.on('parsed',parsed);Codecs.project.on('compile',compiled);
      callbacks=[()=>Codecs.project.removeListener('parsed',parsed),()=>Codecs.project.removeListener('compile',compiled)];
      let action=new Action('toi_workspace_reset',{name:'Tears of Ink: restore split workspace',icon:'view_column',click:installBar});
      MenuBar.addAction(action,'view');actions.push(action);
      if(isWorkspace()) setTimeout(installBar,100);
    },
    onunload() { callbacks.forEach(f=>f());actions.forEach(a=>a.delete());if(bar)bar.remove();if(frame)frame.remove();if(frameObserver)frameObserver.disconnect(); }
  });
})();
