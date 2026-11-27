"""D-105: guaranteed useful chest contents, exact loot expectations and site budgets. Run from repository root."""
import json
import re
from pathlib import Path
root=Path("src/main/resources/data/tears-of-ink/loot_table/chests")
tiers=("poor","common","rich")
names={"ruins_start","ruins_poor","ruins_common","ruins_rich","ruins_inkwood","temple_pale_reward","temple_blood_supply","temple_blood_reward","settlement_supplies","settlement_pantry","gate_supplies","camp_supplies"}
troll_caves={"troll_cave_pale","troll_cave_inkwood"}
names|=troll_caves
names|={"ruins_scattered_"+t for t in (*tiers,"inkwood")}
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
# Each chest has its own unconditional Ink pool and at least two other
# guaranteed item rolls, including one roll of useful crafting materials.
basic_materials={"minecraft:stick","minecraft:iron_ingot","minecraft:charcoal",
                 prefix+"asphoda",prefix+"silk_thread",prefix+"raw_pale_metal",
                 prefix+"raw_blood_metal",prefix+"blood_clot"}
for name,table in tables.items():
    ink_range=(2,4) if name.endswith("_reward") else (2,3) if (
        name.endswith("_rich") or name in {"ruins_inkwood","settlement_pantry"}) else (
        (1,3) if name.endswith("_common") or name=="temple_blood_supply" else (1,2))
    if name.startswith("ruins_scattered_"):
        ink_range={"poor":(3,7),"common":(4,10),"rich":(7,10),"inkwood":(7,10)}[name.removeprefix("ruins_scattered_")]
    if name.startswith(("camp_","watchtower_")):
        ink_range=tuple(5*x for x in ink_range)
    if name in troll_caves: ink_range=(13,20)
    ink_pools=[p for p in table["pools"] if any(e.get("name")==prefix+"ink" for e in p["entries"])]
    assert len(ink_pools)==1, name
    ip=ink_pools[0]
    assert not ip.get("conditions") and ip["rolls"]==1 and len(ip["entries"])==1, name
    ie=ip["entries"][0]
    assert not ie.get("conditions") and ie["type"]=="minecraft:item", name
    assert ie["functions"]==[{"function":"minecraft:set_count","count":{
        "type":"minecraft:uniform","min":ink_range[0],"max":ink_range[1]}}], name
    assert expected(name,"ink")==sum(ink_range)/2, name
    guaranteed=[p for p in table["pools"] if not p.get("conditions")
        and isinstance(p["rolls"],int) and p["rolls"]>=1
        and all(e["type"]=="minecraft:item" and not e.get("conditions") for e in p["entries"])]
    assert sum(p["rolls"] for p in guaranteed)>=3, name
    assert any(all(e.get("name") in basic_materials for e in p["entries"]) for p in guaranteed), name
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
    if name not in {"ruins_inkwood","ruins_scattered_inkwood","troll_cave_inkwood"}: assert expected(name,"raw_ink_metal")==0
    if name not in {"temple_blood_supply","temple_blood_reward","settlement_supplies","settlement_pantry","ruins_poor","ruins_common","ruins_rich","ruins_scattered_poor","ruins_scattered_common","ruins_scattered_rich"}: assert expected(name,"raw_blood_metal")==0
    if name.startswith(("gate_","camp_","watchtower_","settlement_","temple_pale_supply_")) or name=="temple_blood_supply": assert rare_pools==0
for name in ("gate_supplies","camp_supplies","settlement_supplies","settlement_pantry"):
    assert any_item(name,valuable-({"raw_blood_metal"} if name.startswith("settlement_") else set()))==0
    if name.startswith("settlement_"): assert expected(name,"asphoda")==0
assert expected("ruins_start","ink")==1.5 and expected("ruins_start","pigment")==0
assert expected("ruins_start","cooked_caterpillar_meat")==2
assert expected("ruins_rich","raw_pale_metal","inkwood")==0
assert abs(expected("ruins_rich","raw_ink_metal","inkwood")-1.125)<1e-9
assert abs(expected("ruins_inkwood","raw_ink_metal")-1.125)<1e-9
for group in ("ruins_","gate_","gate_cache_","camp_cache_","watchtower_cache_"):
    assert expected(group+"poor","pigment")<expected(group+"common","pigment")<expected(group+"rich","pigment")
for i,t in enumerate(tiers):
    for group,side,counts,maxpig,maxore in (
       ("gate_cache_","gate_supplies",range(1,4),[.08,.12,.15][i],[1.7,2.2,2.7][i]),
       ("camp_cache_","camp_supplies",range(1,3),[.25,.40,.50][i],[6,8.5,11][i]),
       ("watchtower_cache_","camp_supplies",range(1,2),[.30,.50,.60][i],[8.5,11,13.5][i])):
        for count in counts:
            for item,budget in (("pigment",maxpig),("raw_pale_metal",maxore)):
                assert abs(expected(group+t,item)+(count-1)*expected(side,item)-budget)<1e-9
    assert expected("camp_cache_"+t,"pigment")>expected("gate_cache_"+t,"pigment")
    assert expected("gate_"+t,"pigment")<=expected("gate_cache_"+t,"pigment")
    assert expected("temple_pale_supply_"+t,"pigment")==0
assert expected("temple_blood_supply","pigment")==0
assert expected("temple_blood_reward","pigment")==0 and expected("temple_pale_reward","pigment")==0
assert abs(expected("ruins_start","raw_pale_metal")-1.6)<1e-9
assert expected("ruins_start","minecraft:torch")>=1.5
for tier,ore,pig in zip(tiers,(1.0,1.275,1.9),(.05,.08,.10)):
    assert abs(expected("ruins_"+tier,"raw_pale_metal")-ore)<1e-9
    assert abs(expected("ruins_"+tier,"pigment")-pig)<1e-9
    assert expected("ruins_"+tier,"blood_clot")==.05
assert expected("ruins_inkwood","pigment")==.10
for name in tables:
    if name not in {"ruins_inkwood","ruins_scattered_inkwood","troll_cave_inkwood"}:
        assert expected(name,"tearbloom_brew")==0 and expected(name,"strong_tearbloom_brew")==0, name
    if name.startswith("temple_pale") or name=="ruins_start":
        assert expected(name,"blood_clot")==0
assert expected("settlement_supplies","blood_clot")==1.25
assert expected("settlement_pantry","blood_clot")==3
assert abs(expected("settlement_supplies","raw_blood_metal")-.825)<1e-9
assert abs(expected("settlement_pantry","raw_blood_metal")-2.0)<1e-9
assert abs(expected("temple_blood_supply","raw_blood_metal")-1.125)<1e-9
for tier,ore in zip(tiers,(1.0,1.275,1.9)):
    assert abs(expected("temple_pale_supply_"+tier,"raw_pale_metal")-ore)<1e-9
assert abs(any_item("temple_pale_reward",rare)-.65)<1e-9
assert abs(any_item("temple_blood_reward",rare)-.60)<1e-9
for tier, chance in zip(tiers,(.05,.08,.12)):
    name="ruins_"+tier
    assert abs(expected(name,"raw_blood_metal")-chance)<1e-9
    assert expected(name,"raw_blood_metal")<expected(name,"raw_pale_metal")
# New scattered ruins: guaranteed local metal, boosted pigment probability,
# legacy and military tables remain covered by the original assertions.
for tier,metal,ore,pig in (
    ("poor","raw_pale_metal",4.0,.1725),("common","raw_pale_metal",5.0,.276),
    ("rich","raw_pale_metal",7.0,.345),("inkwood","raw_ink_metal",4.5,.345)):
    name="ruins_scattered_"+tier
    old="ruins_"+tier
    assert abs(expected(name,metal)-ore)<1e-9
    assert abs(expected(name,"pigment")-pig)<1e-9
    assert abs(expected(name,"ink")-{"poor":5,"common":7,"rich":8.5,"inkwood":8.5}[tier])<1e-9
    assert abs(any_item(name,rare)-any_item(old,rare)-(.01 if tier=="common" else 0))<1e-9
    metal_pool=next(p for p in tables[name]["pools"] if any(e.get("name")==prefix+metal for e in p["entries"]))
    assert not metal_pool.get("conditions") and metal_pool["rolls"]==1 and len(metal_pool["entries"])==1
    assert expected(name,"raw_blood_metal")==expected(old,"raw_blood_metal")
    assert expected(name,"blood_clot")==expected(old,"blood_clot")
for name,metal,average in (
        ("troll_cave_pale","raw_pale_metal",12.5),("troll_cave_inkwood","raw_ink_metal",10)):
    assert expected(name,metal)==average
    assert any_item(name,{"raw_pale_metal","raw_ink_metal","raw_blood_metal"})==average
    assert abs(expected(name,"pigment")-.45)<1e-9
    assert abs(any_item(name,rare)-.30)<1e-9
    metal_pool=next(p for p in tables[name]["pools"] if any(e.get("name")==prefix+metal for e in p["entries"]))
    assert not metal_pool.get("conditions") and len(metal_pool["entries"])==1 and metal_pool["rolls"]==1
cave=(java/"entity/TrollCave.java").read_text()
assert set(re.findall(r'loot\("([^"]+)"\)',cave))==troll_caves
piece=(java/"world/ruins/RuinPiece.java").read_text()
assert 'tag.getBoolean("EnrichedScattered")' in piece and 'tag.putBoolean("EnrichedScattered",true)' in piece
assert "enrichedScattered ? (pale ? Ruins.SCATTERED_LOOT[tier] : Ruins.SCATTERED_INKWOOD_LOOT)" in piece
structure=(java/"world/ruins/RuinStructure.java").read_text()
assert "!site.temple() && !ScatteredRuinDensity.keep" in structure
dimension=root.parent.parent/"dimension_type"
assert json.loads((dimension/"ink_lands.json").read_text())["fixed_time"]==6000
for mob in ("pale_spearman","pale_crossbowman","pale_shieldbearer"):
    table=json.loads((root.parent/"entities"/(mob+".json")).read_text())
    pool=next(p for p in table["pools"] if any(e.get("name")==prefix+"raw_pale_metal" for e in p["entries"]))
    assert any(c.get("condition")=="minecraft:random_chance" and c.get("chance")==.55 for c in pool["conditions"])
assert "random.nextInt(BloodTuning.CRYSTAL_ONE_IN)" not in (java/"block/blood/BloodTankBlockEntity.java").read_text()
refs=(java/"world/ruins/Ruins.java").read_text()+(java/"world/herald/BloodSettlements.java").read_text()
# Ignore historical strings in comments; actual references must cover every new ID.
assert set(re.findall(r'"chests/([^"]+)"',refs))==names-troll_caves-set("gate_"+t for t in tiers)
assert "i==0?loot:supplies" in (java/"entity/patrol/BarricadeGates.java").read_text()
assert "i==0?loot:supplies" in (java/"entity/patrol/ForestCamps.java").read_text()
assert "q.equals(p.pantry)?PANTRY:LOOT" in (java/"world/herald/BloodSettlements.java").read_text()
assert "Ruins.PALE_TEMPLE_SUPPLIES[tier]" in (java/"world/ruins/RuinPiece.java").read_text()
print(f"D-105: {len(tables)} tables, guaranteed Ink and useful material rolls, rare rewards and bounded site budgets verified")
