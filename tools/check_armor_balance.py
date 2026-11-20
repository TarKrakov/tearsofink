"""D-098: verify actual armor tuning, mixed pieces, tier ordering and armor recipe economy."""
from pathlib import Path
from itertools import product
import json, math, re

src=Path("src/main/java/com/tarkrakov/tearsofink/item/OrdinaryArmorItem.java").read_text()
weights={}
for name in ("LIGHT","MEDIUM","HEAVY"):
    m=re.search(r'\b'+name+r'\("[^"]+", ArmorMaterials\.\w+, ([\d.]+)f?,(.*?)(?=\b(?:MEDIUM|HEAVY)\("|;)',src,re.S)
    assert m, name
    pieces=[(float(a),int(d),float(v)) for a,d,v in re.findall(r'new Piece\(([\d.]+), (\d+), (-?[\d.]+)\)',m[2])]
    assert len(pieces)==4
    weights[name]=(float(m[1]),pieces)
def stats(mix,tier=0):
    return (sum(weights[w][1][i][0]+tier*.25 for i,w in enumerate(mix)),
            sum(weights[w][0]+tier*.25 for w in mix),
            math.prod(1+weights[w][1][i][2] for i,w in enumerate(mix)))
def received(d,a,t):
    return d*(1-min(20,max(a*.2,a-d/(2+t/4)))/25)
light,medium,heavy=names=("LIGHT","MEDIUM","HEAVY")
for w,armor,tough,speed in zip(names,(9,11,13),(1,2,3),(1.10376036,1,.8580316875)):
    a,t,v=stats([w]*4)
    assert a==armor and t==tough and math.isclose(v,speed)
    for tier in (1,2):
        assert stats([w]*4,tier)==(armor+tier,tough+tier,v)
for mix in product(names,repeat=4):
    for tier in (0,1,2):
        a,t,v=stats(mix,tier)
        for i,w in enumerate(mix):
            if w==heavy: continue
            nxt=list(mix)
            nxt[i]=names[names.index(w)+1]
            aa,tt,vv=stats(nxt,tier)
            assert aa>a and tt>t and vv<v
            for d in (4,6,8,10,12,16):
                assert received(d,aa,tt)<received(d,a,t)
for d in (4,6,8,10,12,16):
    l,m,h=[received(d,*stats([w]*4)[:2]) for w in names]
    assert l>m>h and l/h<1.40, (d,l,m,h)
assert math.isclose(received(6,9,1),4.48)
assert math.isclose(received(6,13,3),3.4036363636363633)
root=Path("src/main/resources/data/tears-of-ink/recipe")
regions={
 "pale":[["headband","quilted_jacket","pants","shoes"],["chain_hood","brigandine","reinforced_leggings","leather_boots"],["closed_helmet","cuirass","plate_leggings","sabatons"]],
 "blood":[["veil","robe","skirt","sandals"],["reinforced_mask","padded_mantle","reinforced_skirt","caligae"],["closed_helmet","chainmail","plate_kilt","armored_boots"]],
 "ink":[["woven_hood","leather_jerkin","cloth_pants","travel_boots"],["kettle_hat","scale_armor","padded_leggings","high_boots"],["bascinet","plate_cuirass","plate_leggings","armored_boots"]]
}
armor_ids=set()
for region,sets in regions.items():
    for i,ids in enumerate(sets):
        counts={}
        for name in ids:
            id=region+"_"+name
            armor_ids.add(id)
            recipe=json.loads((root/(id+".json")).read_text())
            assert recipe["type"]=="tears-of-ink:forging" and recipe["result"]["id"]=="tears-of-ink:"+id
            for row in recipe["pattern"]:
                for symbol in row:
                    if symbol==" ": continue
                    key=recipe["key"][symbol]["item"]
                    counts[key]=counts.get(key,0)+1
        expected={"tears-of-ink:"+region+"_ingot":(12,20,22)[i]}
        if i==0: expected["tears-of-ink:silk_thread"]=6
        if i==2: expected["tears-of-ink:asphoda"]=6
        assert counts==expected,(region,i,counts,expected)
def signature(recipe):
    rows=recipe["pattern"]
    if not rows: return None
    if len({len(row) for row in rows})!=1: return None
    cells=[[json.dumps(recipe["key"][c],sort_keys=True) if c!=" " else "" for c in row] for row in rows]
    used=[(y,x) for y,row in enumerate(cells) for x,c in enumerate(row) if c]
    if not used: return None
    y0,y1=min(y for y,x in used),max(y for y,x in used)
    x0,x1=min(x for y,x in used),max(x for y,x in used)
    a=tuple(tuple(row[x0:x1+1]) for row in cells[y0:y1+1])
    b=tuple(tuple(reversed(row)) for row in a)
    return min(a,b)
by_pattern={}
for path in root.glob("*.json"):
    r=json.loads(path.read_text())
    if r.get("type")!="tears-of-ink:forging": continue
    key=signature(r)
    if key is not None: by_pattern.setdefault(key,[]).append(path.stem)
for ids in by_pattern.values():
    assert len(ids)==1 or not armor_ids.intersection(ids), f"armor recipe collision: {ids}"
print("D-098: 36 armor recipes, 81 mixed sets x 3 tiers, damage and speed verified")
