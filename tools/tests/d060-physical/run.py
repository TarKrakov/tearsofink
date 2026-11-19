#!/usr/bin/env python3
"""Physical probe independent of the ten-seed matrix. Fresh V3 and V1 seed 42."""
import argparse
import json
import shutil
import sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parent/"d060"))
import run as d060
from road_samples import read_snapshot, compare_snapshots, snapshot_digest

def install():
    java=d060.JAVA
    for name,target in [
        ("D060PhysicalProbe.java",java/"world/D060PhysicalProbe.java"),
        ("D060PhysicalAdits.java",java/"world/adit/D060PhysicalAdits.java"),
    ]:
        if target.exists(): raise RuntimeError("Test class already exists")
        shutil.copyfile(HERE/name,target)
    edits=[
        (java/"TearsOfInk.java",
         "com.tarkrakov.tearsofink.world.D060Probe.init();",
         "com.tarkrakov.tearsofink.world.D060Probe.init();\n\t\tcom.tarkrakov.tearsofink.world.D060PhysicalProbe.init();"),
        (java/"world/ruins/RuinPiece.java",
         "\tpublic BlockPos startFountain() {",
         """\tpublic BlockPos d060PhysicalFountain() {
        var fountains = template.filterBlocks(templatePosition,
            placeSettings.copy().setBoundingBox(null), ModBlocks.INK_FOUNTAIN);
        return fountains.size()==1?fountains.getFirst().pos():null;
    }

\tpublic BlockPos startFountain() {"""),
        (java/"world/adit/AditPiece.java",
         "\tboolean nearPassage(BlockPos pos) {",
         """\tBlockPos d060Entrance() { return entrance; }
    BlockPos d060Landing() { return landing; }
    java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> d060Interior(ChunkPos chunk) {
        return entrance == null ? java.util.Map.of() : AditTunnel.d060Interior(chunk, variant, sideShift, entrance.getY()-1, entryLane);
    }

\tboolean nearPassage(BlockPos pos) {"""),
        (java/"world/adit/AditTunnel.java",
         "\tstatic AditTunnel at(ChunkPos chunk,",
         """\tstatic Map<BlockPos,BlockState> d060Interior(ChunkPos chunk, int variant, int shift, int y, int lane) {
        return java.util.Map.copyOf(new AditTunnel(chunk,variant,shift,y,lane).interior);
    }

\tstatic AditTunnel at(ChunkPos chunk,"""),
    ]
    for path,old,new in edits:
        text=path.read_text()
        if text.count(old)!=1: raise RuntimeError("Test hook anchor moved: "+str(path))
        path.write_text(text.replace(old,new))

def session(label):
    server=d060.Server(label)
    normal=False
    try:
        server.expect('For help, type "help"',600)
        server.send("toi-d060-probe arrival")
        arrival=json.loads(server.expect("D060_ARRIVAL ",270))
        server.send("toi-d060-physical-full")
        result=json.loads(server.expect("D060_FULL_PHYSICAL ",720))
        result["arrival"]=arrival
        for key,suffix in [("road_blocks_before","-roads-before.tsv"),("road_blocks","-roads.tsv")]:
            road = result.get(key)
            if road is None:
                continue
            # Snapshot names are deliberately removed from the hashed metadata.
            name = road.pop("snapshot")
            if Path(name).name != name or not name.startswith("road-samples-") or not name.endswith(".tsv"):
                raise RuntimeError("Invalid road snapshot filename")
            snapshot = d060.OUT/name
            rows = read_snapshot(snapshot)
            if len(rows) != road["blocks"]:
                raise RuntimeError("Road snapshot count differs from original hash sample")
            if snapshot_digest(rows) != road["sha256"]:
                raise RuntimeError("Road snapshot digest differs from original hash")
            snapshot.rename(d060.OUT/(label+suffix))
        (d060.OUT/(label+".json")).write_text(json.dumps(result,indent=2))
        normal=True
        return result
    finally:
        server.close(normal)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--island-seed",help="Separate physical raised-foundation regression; requires at least one raised temple")
    args=parser.parse_args()
    seed=str(int(args.island_seed)) if args.island_seed is not None else "42"
    original=d060.setup(seed)
    install()
    if args.island_seed is not None:
        first=session("island-v3")
        plan_file=d060.WORLD/"data/tearsofink_temple_network_v3.bin"
        before=d060.digest(plan_file)
        d060.DIM.write_bytes(original)
        restart=session("island-v3-restart")
        after=d060.digest(plan_file)
        # Be explicit if the requested seed doesn't exercise a raised temple.
        # A successful ordinary temple regression is not evidence for the island.
        ok=bool(first.get("raised_foundations")) and first["raised_foundations"]==restart.get("raised_foundations")
        ok=ok and before==after and all(x.get("layout")==3 and x.get("pass") and x["arrival"].get("ready")
                                         for x in (first,restart))
        result={"seed":seed,"pass":ok,"plan_unchanged":before==after,"first":first,"restart":restart}
        (d060.OUT/"island-physical-result.json").write_text(json.dumps(result,indent=2))
        print("D060_ISLAND_SUMMARY "+json.dumps(result),flush=True)
        if not ok: raise SystemExit("D060 raised-foundation physical acceptance failed")
        return
    # First V3, then a separate unvisited V1 save. No reuse of V3 chunks.
    v3=session("physical-v3")
    d060.DIM.write_bytes(original)
    props=d060.RUN/"server.properties"
    props.write_text(props.read_text().replace("level-name=toi-d060-disposable","level-name=toi-d060-legacy"))
    legacy=d060.RUN/"toi-d060-legacy"
    if legacy.exists(): raise RuntimeError("Unexpected existing V1 world")
    v1=session("physical-v1")
    # Restart established V1 with the experimental definition enabled in the
    # isolated checkout; marker must still pin V1, including new worldgen.
    dim=json.loads(original)
    dim["generator"]["biome_source"]["temple_layout"]=3
    d060.DIM.write_text(json.dumps(dim))
    v1restart=session("physical-v1-restart")
    d060.DIM.write_bytes(original)
    props.write_text(props.read_text().replace("level-name=toi-d060-legacy","level-name=toi-d060-legacy-control"))
    control=d060.RUN/"toi-d060-legacy-control"
    if control.exists(): raise RuntimeError("Unexpected existing V1 control world")
    v1control=session("physical-v1-control")
    comparison={
        "layouts": [v3.get("layout"),v1.get("layout"),v1restart.get("layout")],
        "control_layout": v1control.get("layout"),
        "v1_control_same_temples": v1.get("temples")==v1control.get("temples"),
        "v1_control_same_adits": v1.get("adits")==v1control.get("adits"),
        "v1_control_same_arrival": v1["arrival"].get("position")==v1control["arrival"].get("position"),
        "v1_control_exact_road_match": v1.get("road_blocks")==v1control.get("road_blocks"),
        "same_starter": bool(v3.get("starter_fountain")) and v3.get("starter_fountain")==v1.get("starter_fountain")==v1restart.get("starter_fountain"),
        "same_starter_bbox": bool(v3.get("starter_bbox")) and v3.get("starter_bbox")==v1.get("starter_bbox")==v1restart.get("starter_bbox"),
        "same_arrival": bool(v3["arrival"].get("position")) and v3["arrival"].get("position")==v1["arrival"].get("position")==v1restart["arrival"].get("position"),
        "same_sampled_road_blocks": bool(v3.get("road_blocks")) and v3.get("road_blocks")==v1.get("road_blocks")==v1restart.get("road_blocks"),
        "same_road_blocks_before_structures": bool(v3.get("road_blocks_before")) and v3.get("road_blocks_before")==v1.get("road_blocks_before"),
        "road_blocks_stable_after_structures": {k:bool(x.get("road_blocks_before")) and x["road_blocks_before"]==x.get("road_blocks") for k,x in [("v3",v3),("v1",v1),("v1_restart",v1restart)]},
        "v1_unchanged_temples": bool(v1.get("temples")) and v1.get("temples")==v1restart.get("temples"),
        "v1_unchanged_adits": bool(v1.get("adits")) and v1.get("adits")==v1restart.get("adits"),
        "timings_ms": {k: x["arrival"].get("elapsed_ms") for k,x in [("v3",v3),("v1",v1),("v1_restart",v1restart)]}
    }
    # Locate every changed block while retaining the original strict pass/fail.
    diagnostics = {}
    late = [(a+"-roads",b+"-roads",a+"-vs-"+b) for a,b in
            [("physical-v3","physical-v1"),("physical-v1","physical-v1-restart"),("physical-v1","physical-v1-control")]]
    early = [(a+"-roads-before",b+"-roads-before",a+"-vs-"+b+"-before-structures") for a,b in
             [("physical-v3","physical-v1"),("physical-v1","physical-v1-restart"),("physical-v1","physical-v1-control")]]
    within = [(a+"-roads-before",a+"-roads",a+"-before-vs-after-structures") for a in
              ["physical-v3","physical-v1","physical-v1-restart"]]
    for lhs,rhs,name in late+early+within:
        left,right = (d060.OUT/(label+".tsv") for label in (lhs,rhs))
        if left.exists() and right.exists():
            diff = compare_snapshots(read_snapshot(left),read_snapshot(right))
            (d060.OUT/(name+".json")).write_text(json.dumps(diff,indent=2))
            diagnostics[name] = {k:v for k,v in diff.items() if k!="differences"}
            diagnostics[name]["examples"] = diff["differences"][:8]
    result={"comparison":comparison,"road_diagnostics":diagnostics,"v3":v3,"v1":v1,"v1_restart":v1restart,"v1_control":v1control}
    (d060.OUT/"full-physical-result.json").write_text(json.dumps(result,indent=2))
    print("D060_FULL_SUMMARY "+json.dumps(result),flush=True)
    ok=all(x.get("pass") and x["arrival"].get("ready") and x.get("seed")=="42" for x in [v3,v1,v1restart])
    ok=ok and comparison["layouts"]==[3,1,1]
    ok=ok and comparison["control_layout"]==1 and v1control["arrival"].get("ready") and v1control.get("seed")=="42"
    ok=ok and all(comparison[k] for k in ["same_starter","same_starter_bbox","same_arrival","same_sampled_road_blocks","v1_unchanged_temples","v1_unchanged_adits"])
    if not ok: raise SystemExit("D060 physical/baseline acceptance failed")
if __name__=="__main__": main()
