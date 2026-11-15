import sys, json, copy; sys.path.insert(0,'.')
import run, tl
from run import *
run.ITEM['bastion']='shield/ram'
def K(t,r,tr,e="linear"): return {"t":t,"rotation":r,"translation":tr,"ease":e}
VAR={}
# A — плоскостью: толчок лицом щита
VAR['flat']={
 'bash':{"length":0.7,"hold":0.25,"return":0.3,"impact":0.18,"smooth":True,"keyframes":[
   K(0,[0,0,0],[0,0,0]), K(0.1,[8,-10,0],[2,-1,4]), K(0.18,[-6,15,0],[-4,1,-9]), K(0.26,[-4,14,0],[-4,1,-8])]},
 'slam':{"length":0.9,"hold":0.25,"return":0.35,"impact":0.3,"smooth":True,"keyframes":[
   K(0,[0,0,0],[0,0,0]), K(0.16,[35,0,0],[0,9,3]), K(0.3,[-40,8,0],[-2,-4,-10]), K(0.4,[-42,8,0],[-2,-5,-9])]}}
# B — кромкой: щит развёрнут ребром, рубит наискосок
VAR['edge']={
 'bash':{"length":0.7,"hold":0.25,"return":0.3,"impact":0.18,"smooth":True,"keyframes":[
   K(0,[0,0,0],[0,0,0]), K(0.1,[20,-60,-20],[6,4,2]), K(0.18,[-20,-80,30],[-5,-2,-7]), K(0.26,[-22,-82,32],[-6,-3,-6])]},
 'slam':{"length":0.9,"hold":0.25,"return":0.35,"impact":0.3,"smooth":True,"keyframes":[
   K(0,[0,0,0],[0,0,0]), K(0.16,[50,-85,0],[0,9,3]), K(0.3,[-55,-85,0],[-2,-5,-9]), K(0.4,[-57,-85,0],[-2,-6,-8])]}}
def make(var, out):
    a={'animations':VAR[var]}
    model=json.load(open(REPO+'/models/item/shield/ram.json'))
    orig=run.load
    tl.load=lambda arch: (a, model, model['display']['firstperson_righthand'], model['display']['thirdperson_righthand'], None)
    tl.COMBO_OVERRIDE=True
    steps=[('bash',None,0.6),(None,'bash',0.6),('slam','slam',0.9)]
    # своё расписание без weapon_combos
    tl.json_load=json.load
    import types
    sched=[]; T=0
    for m_,o_,dur in steps:
        if m_: sched.append((T,1,m_))
        if o_: sched.append((T,-1,o_))
        T+=dur
    frames=[]
    fps=20
    for i in range(int((T+0.6)*fps)):
        now=i/fps
        arms={'R':VANILLA_ARM,'L':VANILLA_ARM}; items={}; fq=[]; labels=[]
        act={}
        for st,m,nm in sched:
            if st<=now: act[m]=(st,nm)
        disp=model['display']
        for s,m in (('R',1),('L',-1)):
            side='right' if m==1 else 'left'
            tp=disp['thirdperson_'+side+'hand']
            if m in act and (now-act[m][0])<end_time(a['animations'][act[m][1]]):
                c=a['animations'][act[m][1]]; t=now-act[m][0]
                arm,W,tp_,handle,q,by,P,w=tl.hand_pose('bastion',model,None,c,t,m,TUNE)
                arms[s]=arm; items[s]=(model,(lambda tp,W,m,h:(lambda am: tp_item_m(am,tp,h,W,m)))(tp,W,m,handle)); fq+=q; labels.append(act[m][1])
            else:
                items[s]=(model,(lambda tp,m:(lambda am: tp_item_m(am,tp,None,None,m)))(tp,m))
                fq+=item_quads(model, fp_item_m(disp['firstperson_'+side+'hand'],np.zeros(3),np.zeros(3),None,m),(190,40,50) if m==1 else (150,30,90))
        q=scene(arms,items,0)
        A_=render(fq,[0,0,0],[0,0,-1],size=(240,180),fov=70,bg=(40,40,56))
        B_=render(q,[0.9,2.4,2.6],[0,1.3,-0.6],size=(240,240),fov=50)
        C_=render(q,[-1.8,1.9,-2.4],[0,1.25,0],size=(240,240),fov=50)
        im=Image.new('RGB',(720,240),(20,20,20)); im.paste(B_,(0,0)); im.paste(C_,(240,0)); im.paste(A_,(480,30))
        dr=ImageDraw.Draw(im); dr.text((484,214),f'{var}: '+' + '.join(labels),fill=(255,255,255),font=tl.FONT); dr.text((484,2),'от первого лица',fill=(200,200,200),font=tl.FONT)
        frames.append(im)
    frames[0].save(out,save_all=True,append_images=frames[1:],duration=50,loop=0)
if __name__=='__main__':
    for v in VAR: make(v, f'/home/claude/preview/out/bastion_{v}.gif')
