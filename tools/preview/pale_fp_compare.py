"""Reconstruct old equip-dependent FP versus the corrected camera-space poses.
This is a technical render, not a screenshot from a running Minecraft client.
"""
import subprocess
from pathlib import Path
from tl import *
out=Path(sys.argv[1]if len(sys.argv)>1 else'out/pale');out.mkdir(parents=True,exist_ok=True)
a,m,_,_,g=load('pale_linear_greatsword');old=json.loads(subprocess.check_output(['git','show','28a2a80:src/main/resources/assets/tears-of-ink/weapon_anims/pale_linear_greatsword.json'],text=True))
canvas=Image.new('RGB',(960,1160),(20,20,20));labels=['ДО: прежние FP-позы, equip=1 справа / 0 слева','ПОСЛЕ: поднятые FP-позы, equip=0 в обеих руках']
for row,data in enumerate((old,a)):
 quads=[]
 for sign,suffix in((1,''),(-1,'_off')):
  clip=data['animations']['axe_dual_slam'+suffix];r,p=sample(clip,.2);f=m['display']['firstperson_'+('right'if sign==1 else'left')+'hand']
  eq=1 if row==0 and sign==1 else 0
  quads+=item_quads(m,fp_item_m(f,r,p,g,sign,equipped=eq),(190,40,50)if sign==1 else(150,30,90))
 im=render(quads,[0,0,0],[0,0,-1],(960,540),70,bg=(40,40,56));d=ImageDraw.Draw(im)
 d.line((472,270,488,270),fill=(225,225,225));d.line((480,262,480,278),fill=(225,225,225));d.line((0,500,960,500),fill=(80,80,90))
 canvas.paste(im,(0,row*580+40));ImageDraw.Draw(canvas).text((10,row*580+10),labels[row],font=FONT,fill=(240,240,240))
canvas.save(out/'fp-before-after.png')
