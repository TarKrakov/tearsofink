"""Build the separate Blockbench authoring rig from the unchanged authored sword.
Units are model pixels. Camera-space FP and renderer-world F5 are separate rigs.
"""
import base64,copy,hashlib,io,json,sys,uuid
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools/preview'))
from run import load
from third import *
from hilt import euler_xyz
OUT=Path(__file__).parent
NS=uuid.UUID('d654542e-947b-46e0-9d90-8bbfa7163b3a')
def uid(n):return str(uuid.uuid5(NS,n))
def euler_zyx(R):
 y=math.asin(float(np.clip(-R[2,0],-1,1)))
 if abs(math.cos(y))>1e-7:x=math.atan2(R[2,1],R[2,2]);z=math.atan2(R[1,0],R[0,0])
 else:x=math.atan2(-R[1,2],R[1,1]);z=0
 return np.degrees([x,y,z])
def generate():
 a,m,fp,tp,g=load('pale_linear_greatsword');source_path=ROOT/'src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel';source=json.loads(source_path.read_text())
 groups=[];elements=[];outliner=[];nodes={};children={}
 def group(name,origin,parent=None,rotation=(0,0,0),opened=True):
  obj=dict(name=name,uuid=uid(name),origin=list(origin),rotation=list(rotation),export=True,visibility=True,locked=False,isOpen=opened,color=0)
  groups.append(obj);nodes[name]=obj;children[name]=[]
  entry={'uuid':obj['uuid'],'isOpen':opened,'children':children[name]}
  (children[parent]if parent else outliner).append(entry);return obj
 def cube(name,lo,hi,parent,tile,rotation=(0,0,0),origin=None):
  u=16+(tile%4)*4;v=(tile//4)*4
  obj=dict(name=name,uuid=uid(name),type='cube',from_=list(lo),to=list(hi),origin=list(origin if origin is not None else(np.array(lo)+hi)/2),rotation=list(rotation),box_uv=False,locked=True,export=True,visibility=True,autouv=0,
           faces={f:{'uv':[u+.1,v+.1,u+3.9,v+3.9],'texture':0}for f in('north','east','south','west','up','down')})
  obj['from']=obj.pop('from_');elements.append(obj);children[parent].append(obj['uuid']);return obj
 group('F5_RIG',[0,0,0]);group('F5_body',[0,12,0],'F5_RIG');group('F5_head',[0,24.016,0],'F5_body',opened=False)
 cube('F5_torso',[-4,12.016,-2],[4,24.016,2],'F5_body',1)
 cube('F5_head_guide',[-4,24.016,-4],[4,32.016,4],'F5_head',0)
 for side,sgn in(('right',1),('left',-1)):
  cube('F5_'+side+'_leg',[sgn*2-2,.016,-2],[sgn*2+2,12.016,2],'F5_RIG',2)
 group('FP_RIG',[0,0,0],opened=False);group('FP_body',[0,0,0],'FP_RIG')
 bindings={}
 for view in('F5','FP'):
  for side,sign in(('right',1),('left',-1)):
   d=m['display'][('thirdperson_'if view=='F5'else'firstperson_')+side+'hand']
   if view=='F5':
    P=apply4(WORLD,np.array([[-sign*5,2,0]])/16)[0]*16
    mm=tp_item_m(WORLD@part_m([-sign*5,2,0],VANILLA_ARM[0],0,0),d,g,None,sign)
   else:
    mount=np.array(a.get('firstperson_offset',[0,0,0]))+np.array(a.get('firstperson_idle_offset',[0,0,0]))
    mm=fp_item_m(d,np.zeros(3),np.zeros(3),g,sign,view_offset=mount)
   H=apply4(mm,np.array([g])/16)[0]*16
   if view=='FP':P=H+np.array([0,10,4])
   baseR=mm[:3,:3]/dS(d)[0]
   parent=view+'_body';shoulder=view+'_'+side+'_shoulder';arm=view+'_'+side+'_arm';sword=view+'_'+side+'_sword'
   group(shoulder,P,parent);group(arm,P,shoulder);group(sword,H,arm,euler_zyx(baseR).round(8),False)
   # Solid arm guide follows the measured shoulder->grip segment; not a Minecraft skin.
   direction=(H-P)/np.linalg.norm(H-P);down=np.array([0.,-1,0]);v=np.cross(down,direction);c=down@direction
   K=np.array([[0,-v[2],v[1]],[v[2],0,-v[0]],[-v[1],v[0],0]])
   rr=np.eye(3)+K+K@K/(1+c)if c>-.99999 else Rx(math.pi)
   L=np.linalg.norm(H-P)
   cube(view+'_'+side+'_forearm',P+[-1.7,-L,-1.7],P+[1.7,0,1.7],arm,3 if sign==1 else 4,euler_zyx(rr),P)
   cube(view+'_'+side+'_palm',H-1.6,H+1.6,arm,0)
   scale=float(dS(d)[0])
   for i,original in enumerate(source['elements']):
    el=copy.deepcopy(original);el.update(name=sword+'_cube_'+str(i),uuid=uid(sword+'_cube_'+str(i)),locked=True,visibility=True,export=True)
    for k in('from','to','origin'):el[k]=(H+scale*(np.array(original[k])-g)).round(8).tolist()
    elements.append(el);children[sword].append(el['uuid'])
   bindings[view+'_'+side]={'arm':arm,'shoulder':shoulder,'sword':sword,'pivot':P.tolist(),'grip_position':H.tolist(),'item_rotation':baseR.tolist(),'scale':scale}
 # Hide ALL FP objects by default; no reliance on inherited visibility serialization.
 for obj in groups+elements:
  if obj['name'].startswith('FP_'):obj['visibility']=False
 # Bedrock Animated Model uses ONE render texture: append solid guide pixels
 # to an atlas, retaining every original sword texel and every sword UV coordinate.
 original=Image.open(io.BytesIO(base64.b64decode(source['textures'][0]['source'].split(',',1)[1]))).convert('RGBA')
 assert original.size==(32,32),'Recalibrate atlas UV origin for a changed author texture'
 atlas=Image.new('RGBA',(64,32),(255,255,255,255));atlas.paste(original,(0,0))
 colors=[(190,155,120),(55,90,145),(65,70,85),(190,130,85),(155,150,110)]
 for i,col in enumerate(colors):
  for y in range((i//4)*8,(i//4)*8+8):
   for x in range(32+(i%4)*8,32+(i%4)*8+8):atlas.putpixel((x,y),col+(255,))
 data=io.BytesIO();atlas.save(data,format='PNG')
 texture=copy.deepcopy(source['textures'][0]);texture.update(name='TOI_editor_atlas.png',mode='bitmap',id='0',path='',width=64,height=32,uv_width=32,uv_height=16,source='data:image/png;base64,'+base64.b64encode(data.getvalue()).decode())
 textures=[texture]
 # Cameras are native elements supplied by JannisX11's Cameras plugin.
 cameras=[dict(name='TOI_FP_70',position=[0,0,0],rotation=[0,0,0],fov=70),dict(name='TOI_F5_front',position=[0,25,-55],rotation=[-10,180,0],fov=50),dict(name='TOI_F5_back',position=[0,25,55],rotation=[-10,0,0],fov=50)]
 for c in cameras:
  c.update(uuid=uid(c['name']),type='camera',aspect_ratio=[16,9],linked_preview='',camera_linked=False,visibility=False,export=False);elements.append(c);outliner.append(c['uuid'])
 def kf(channel,t,xyz,name):return dict(uuid=uid(name+channel+str(t)),channel=channel,time=t,interpolation='linear',data_points=[dict(zip(('x','y','z'),map(str,xyz)))])
 animations=[]
 for dual in(False,True):
  name='animation.toi.demo_'+('dual'if dual else'single')+'_guard';animators={}
  for view in('FP','F5'):
   for side,sign in(('right',1),('left',-1)):
    if not dual and sign==-1:continue
    # Deliberately separate shoulder and wrist keys: the artist can grab either bone.
    for part,target in(('arm',[-30,-sign*8,sign*12]),('sword',[sign*9,sign*-12,sign*20])):
     bone=view+'_'+side+'_'+part
     animators[uid(bone)]={'name':bone,'type':'bone','rotation_global':False,'quaternion_interpolation':False,'keyframes':[kf('rotation',t,v,bone+name)for t,v in((0,[0,0,0]),(.25,target),(.75,target),(1,[0,0,0]))]}
  animations.append(dict(uuid=uid(name),name=name,loop='once',override=False,length=1,snapping=20,selected=False,saved=True,animators=animators))
 manifest={'version':1,'archetype':'pale_linear_greatsword','euler_order':'ZYX','grip':g.tolist()if hasattr(g,'tolist')else g,'bindings':bindings,'display':m['display'],'camera_names':[c['name']for c in cameras],'fp_mount':a.get('firstperson_offset',[0,0,0]),'fp_idle_offset':a.get('firstperson_idle_offset',[0,0,0]),'source_sha256':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in(source_path,ROOT/'src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.json')},'calibration':'renderer-math only; live Minecraft comparison pending'}
 model=dict(meta={'format_version':'5.0','model_format':'bedrock','box_uv':False},name='Tears of Ink — weapon animation workspace',model_identifier='toi_weapon_workspace',visible_box=[8,4,1],resolution={'width':32,'height':16},elements=elements,groups=groups,outliner=outliner,textures=textures,animations=animations,toi_workspace=manifest,
 editor_state={'mode':'edit','tool':'rotate_tool','selected_elements':[],'selected_groups':[],'previews':{'main':{'position':[45,30,55],'target':[0,16,0],'orthographic':False,'zoom':1}}})
 (OUT/'pale_animation_workspace.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
 (OUT/'pale_workspace_rig.json').write_text(json.dumps(manifest,indent=2)+'\n')
 return model
if __name__=='__main__':generate()
