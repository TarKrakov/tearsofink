import sys
from PIL import Image, ImageSequence
im=Image.open(sys.argv[1]); fr=[f.convert('RGB') for f in ImageSequence.Iterator(im)]
step=max(1,len(fr)//12); sel=fr[::step][:12]; W,H=fr[0].size
out=Image.new('RGB',(W*2,H*6))
for i,f in enumerate(sel): out.paste(f,((i%2)*W,(i//2)*H))
out.save(sys.argv[2])
