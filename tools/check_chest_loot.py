"""Static D-088 chest loot contract; run from repository root."""
import json
from pathlib import Path
root = Path("src/main/resources/data/tears-of-ink/loot_table/chests")
names = {"ruins_start","ruins_poor","ruins_common","ruins_rich","ruins_inkwood",
         "temple_pale_reward","temple_blood_supply","temple_blood_reward","gate_poor","gate_common","gate_rich"}
tables = {n: json.loads((root/(n+".json")).read_text()) for n in names}
prefix = "tears-of-ink:"
def mean(n):
    return n if isinstance(n,(int,float)) else (n["min"]+n["max"])/2
def expected(name,item,biome="pale"):
    total = 0
    for pool in tables[name]["pools"]:
        q = 1
        for c in pool.get("conditions",[]):
            if c["condition"] == "minecraft:random_chance": q *= c["chance"]
            elif c["condition"] == "minecraft:location_check": q *= biome == "inkwood"
            elif c["condition"] == "minecraft:inverted": q *= biome != "inkwood"
            else: raise AssertionError(c)
        weight = sum(e["weight"] for e in pool["entries"])
        for e in pool["entries"]:
            if e.get("name") == prefix+item:
                total += q*mean(pool["rolls"])*e["weight"]/weight*mean(e.get("functions",[{"count":1}])[0]["count"])
    return total
rare = {"skill_book_dash","skill_book_ink_jump","skill_book_stride","skill_book_ascension","hand_lantern","bright_hand_lantern","atonement"}
for name, table in tables.items():
    assert table["type"]=="minecraft:chest" and table["random_sequence"]==prefix+"chests/"+name
    rare_pools = 0
    for pool in table["pools"]:
        assert pool["entries"] and all(isinstance(e["weight"],int) and e["weight"]>0 for e in pool["entries"])
        assert all(e["type"] in ("minecraft:item","minecraft:empty") for e in pool["entries"])
        if any(e.get("name","").removeprefix(prefix) in rare for e in pool["entries"]):
            rare_pools += 1
            assert pool["rolls"]==1
        for e in pool["entries"]:
            assert e.get("name","").removeprefix(prefix) not in {"valor_sigil","crimson_flower","hope_mote","madness_shard","ink_anvil","cursed_altar"}
        for c in pool.get("conditions",[]):
            if c["condition"]=="minecraft:random_chance": assert 0<=c["chance"]<=1
    assert rare_pools<=1
    if name!="ruins_inkwood": assert expected(name,"raw_ink_metal")==0
    if name.startswith("gate_"): assert rare_pools==0 and expected(name,"raw_pale_metal")>=1
    if name not in {"temple_blood_supply","temple_blood_reward"}: assert expected(name,"raw_blood_metal")==0
assert expected("ruins_start","ink")==1 and expected("ruins_start","pigment")==0
assert expected("ruins_start","cooked_caterpillar_meat")==2
assert expected("ruins_rich","raw_pale_metal","inkwood")==0
assert abs(expected("ruins_rich","raw_ink_metal","inkwood")-.975)<1e-9
assert abs(expected("ruins_inkwood","raw_ink_metal")-.975)<1e-9
for tier in ("poor","common","rich"): assert expected("gate_"+tier,"pigment")>expected("ruins_"+tier,"pigment")
for group in ("ruins_","gate_"):
    assert expected(group+"poor","pigment")<expected(group+"common","pigment")<expected(group+"rich","pigment")
print("D-088: 11 chest tables, regional ore, single rare roll and progression verified")
