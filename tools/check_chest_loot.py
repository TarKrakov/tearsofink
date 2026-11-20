"""D-093: exact loot expectations and site budgets. Run from repository root."""
import json
import re
from pathlib import Path
root=Path("src/main/resources/data/tears-of-ink/loot_table/chests")
tiers=("poor","common","rich")
names={"ruins_start","ruins_poor","ruins_common","ruins_rich","ruins_inkwood","temple_pale_reward","temple_blood_supply","temple_blood_reward","settlement_supplies","settlement_pantry","gate_supplies","camp_supplies"}
names|={pre+t for pre in ("gate_","gate_cache_","camp_cache_","watchtower_cache_","temple_pale_supply_") for t in tiers}
tables={p.stem:json.loads(p.read_text()) for p in root.glob("*.json")}
assert set(tables)==names
prefix="tears-of-ink:"
rare={"skill_book_dash","skill_book_ink_jump","skill_book_stride","skill_book_ascension","hand_lantern","bright_hand_lantern","atonement"}
blocked={"valor_sigil","crimson_flower","hope_mote","hope_essence","madness_shard","ink_echo","blood_crystal","crimson_gift","ink_anvil","cursed_altar"}
valuable=rare|{"pigment","raw_pale_metal","raw_blood_metal","raw_ink_metal"}
java=Path("src/main/java/com/tarkrakov/tearsofink")
registered=set(re.findall(r'register\("([^"]+)"', (java/"item/ModItems.java").read_text()+(java/"block/ModBlocks.java").read_text()))
def mean(n):
    return n if isinstance(n,(int,float)) else (n["min"]+n["max"])/2
def expected(name,item,biome="pale"):
    total=0
    for pool in tables[name]["pools"]:
        q=1
        for c in pool.get("conditions",[]):
            if c["condition"]=="minecraft:random_chance": q*=c["chance"]
            elif c["condition"]=="minecraft:location_check": q*=biome=="inkwood"
            elif c["condition"]=="minecraft:inverted": q*=biome!="inkwood"
            else: raise AssertionError(c)
        weight=sum(e["weight"] for e in pool["entries"])
        for e in pool["entries"]:
            if e.get("name")== (item if ":" in item else prefix+item):
                total+=q*mean(pool["rolls"])*e["weight"]/weight*mean(e.get("functions",[{"count":1}])[0]["count"])
    return total
def any_item(name,items):
    return sum(expected(name,k) for k in items)
for name,table in tables.items():
    assert table["type"]=="minecraft:chest" and table["random_sequence"]==prefix+"chests/"+name
    rare_pools=0
    for pool in table["pools"]:
        assert pool["entries"] and all(isinstance(e["weight"],int) and e["weight"]>0 for e in pool["entries"])
        assert all(e["type"] in ("minecraft:item","minecraft:empty") for e in pool["entries"])
        if any(e.get("name","").removeprefix(prefix) in rare for e in pool["entries"]):
            rare_pools+=1
            assert pool["rolls"]==1
        for e in pool["entries"]:
            k=e.get("name","")
            assert k.removeprefix(prefix) not in blocked
            if k.startswith(prefix): assert k.removeprefix(prefix) in registered, k
        for c in pool.get("conditions",[]):
            if c["condition"]=="minecraft:random_chance": assert 0<=c["chance"]<=1
    assert rare_pools<=1
    if name!="ruins_inkwood": assert expected(name,"raw_ink_metal")==0
    if name not in {"temple_blood_supply","temple_blood_reward"}: assert expected(name,"raw_blood_metal")==0
    if name.startswith(("gate_","camp_","watchtower_","settlement_","temple_pale_supply_")) or name=="temple_blood_supply": assert rare_pools==0
for name in ("gate_supplies","camp_supplies","settlement_supplies","settlement_pantry"):
    assert any_item(name,valuable)==0
    if name.startswith("settlement_"): assert expected(name,"asphoda")==0 and expected(name,"ink")==0
assert expected("ruins_start","ink")==1 and expected("ruins_start","pigment")==0
assert expected("ruins_start","cooked_caterpillar_meat")==2
assert expected("ruins_rich","raw_pale_metal","inkwood")==0
assert abs(expected("ruins_rich","raw_ink_metal","inkwood")-.975)<1e-9
assert abs(expected("ruins_inkwood","raw_ink_metal")-.975)<1e-9
for group in ("ruins_","gate_","gate_cache_","camp_cache_","watchtower_cache_"):
    assert expected(group+"poor","pigment")<expected(group+"common","pigment")<expected(group+"rich","pigment")
for i,t in enumerate(tiers):
    for group,side,counts,maxpig,maxore in (
       ("gate_cache_","gate_supplies",range(1,4),[.08,.12,.15][i],[1.5,2,2.5][i]),
       ("camp_cache_","camp_supplies",range(1,3),[.05,.08,.10][i],[1,1.5,2][i]),
       ("watchtower_cache_","camp_supplies",range(1,2),[.06,.10,.12][i],[1.5,2,2.5][i])):
        for count in counts:
            for item,budget in (("pigment",maxpig),("raw_pale_metal",maxore)):
                assert abs(expected(group+t,item)+(count-1)*expected(side,item)-budget)<1e-9
    assert expected("gate_cache_"+t,"pigment")>expected("camp_cache_"+t,"pigment")
    assert expected("gate_"+t,"pigment")<=expected("gate_cache_"+t,"pigment")
    assert expected("temple_pale_supply_"+t,"pigment")==0
assert expected("temple_blood_supply","pigment")==0
assert expected("temple_blood_reward","pigment")==0 and expected("temple_pale_reward","pigment")==0
assert abs(expected("ruins_start","raw_pale_metal")-1.45)<1e-9
assert expected("ruins_start","minecraft:torch")>=1.5
for tier,ore,pig in zip(tiers,(.8,1.125,1.7),(.05,.08,.10)):
    assert abs(expected("ruins_"+tier,"raw_pale_metal")-ore)<1e-9
    assert abs(expected("ruins_"+tier,"pigment")-pig)<1e-9
    assert expected("ruins_"+tier,"blood_clot")==.05
assert expected("ruins_inkwood","pigment")==.10
for name in tables:
    if name!="ruins_inkwood":
        assert expected(name,"tearbloom_brew")==0 and expected(name,"strong_tearbloom_brew")==0, name
    if name.startswith("temple_pale") or name=="ruins_start":
        assert expected(name,"blood_clot")==0
assert expected("settlement_supplies","blood_clot")==1.25
assert expected("settlement_pantry","blood_clot")==3
for mob in ("pale_spearman","pale_crossbowman","pale_shieldbearer"):
    table=json.loads((root.parent/"entities"/(mob+".json")).read_text())
    pool=next(p for p in table["pools"] if any(e.get("name")==prefix+"raw_pale_metal" for e in p["entries"]))
    assert any(c.get("condition")=="minecraft:random_chance" and c.get("chance")==.55 for c in pool["conditions"])
assert "random.nextInt(BloodTuning.CRYSTAL_ONE_IN)" not in (java/"block/blood/BloodTankBlockEntity.java").read_text()
refs=(java/"world/ruins/Ruins.java").read_text()+(java/"world/herald/BloodSettlements.java").read_text()
# Ignore historical strings in comments; actual references must cover every new ID.
assert set(re.findall(r'"chests/([^"]+)"',refs))==names-set("gate_"+t for t in tiers)
assert "i==0?loot:supplies" in (java/"entity/patrol/BarricadeGates.java").read_text()
assert "i==0?loot:supplies" in (java/"entity/patrol/ForestCamps.java").read_text()
assert "q.equals(p.pantry)?PANTRY:LOOT" in (java/"world/herald/BloodSettlements.java").read_text()
assert "Ruins.PALE_TEMPLE_SUPPLIES[tier]" in (java/"world/ruins/RuinPiece.java").read_text()
print(f"D-101: {len(tables)} tables, regional loot and bounded site budgets verified")
