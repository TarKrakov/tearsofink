"""Compare inherited Display before/after alias conflict removal, with identical geometry.
No model file is edited. Output is a technical FP/F5 baseline capture.
"""
import copy,json,subprocess,sys
from pathlib import Path
from tl import *
a,current,fp,tp,grip=load('pale_linear_greatsword')
out=Path(sys.argv[1] if len(sys.argv)>1 else 'out/pale');out.mkdir(parents=True,exist_ok=True)
alias='src/main/resources/assets/tears-of-ink/models/item/pale_linear_greatsword.json'
old=json.loads(subprocess.check_output(['git','show','eed44c2:'+alias],text=True))
before=copy.deepcopy(current);before['display'].update(old.get('display',{}))
def baseline(model):
    items={};fq=[]
    for side,sign in(('R',1),('L',-1)):
        name='right' if sign==1 else 'left';tp=model['display']['thirdperson_'+name+'hand'];fp=model['display']['firstperson_'+name+'hand']
        items[side]=(model,lambda arm,tp=tp,sign=sign:tp_item_m(arm,tp,None,None,sign))
        fq+=item_quads(model,fp_item_m(fp,np.zeros(3),np.zeros(3),grip,sign),(190,40,50)if sign==1 else(150,30,90))
    q=scene({'R':VANILLA_ARM,'L':VANILLA_ARM},items,0)
    row=Image.new('RGB',(960,320),(20,20,20))
    row.paste(render(q,[.9,2.4,2.6],[0,1.3,-.6],(320,320),50),(0,0))
    row.paste(render(q,[0,1.7,-3.3],[0,1.25,0],(320,320),50),(320,0))
    row.paste(render(fq,[0,0,0],[0,0,-1],(320,240),70),(640,40))
    return row
sheet=Image.new('RGB',(960,688),(20,20,20));d=ImageDraw.Draw(sheet)
for i,(model,label)in enumerate(((before,'ДО: новые модели, старое левое переопределение alias'),(current,'ПОСЛЕ: точные авторские Display обеих рук'))):
    d.text((8,i*344+4),label,font=FONT,fill=(255,255,255));sheet.paste(baseline(model),(0,i*344+24))
sheet.save(out/'baseline-alias-comparison.png');baseline(current).save(out/'baseline.png')
