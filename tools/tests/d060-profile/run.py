#!/usr/bin/env python3
"""Paired CPU/wall profile of the accepted planner and the exact-query cache.

Only runs in a disposable Actions checkout. Neither production nor saved
author worlds are instrumented. Both variants run on the same runner.
"""
import argparse
from collections import Counter
import json
from pathlib import Path
import subprocess
import sys

HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parent/"d060"))
import run as d060

BASELINE="aa72a27b67f3bfeae30262215869099a9d0fa966"

def summarize(path):
    data=json.loads(subprocess.check_output(
        ["jfr","print","--json","--stack-depth","64","--events",
         "jdk.ExecutionSample,jdk.NativeMethodSample",str(path)],text=True))
    top, inclusive=Counter(),Counter()
    samples=0
    for event in data["recording"]["events"]:
        frames=(event["values"].get("stackTrace") or {}).get("frames",[])
        names=[]
        for frame in frames:
            method=frame.get("method") or {}
            owner=(method.get("type") or {}).get("name","").replace("/",".")
            names.append(owner+"."+method.get("name","?"))
        if not any("tearsofink.world.TempleNetwork" in name for name in names):
            continue
        samples+=1
        top.update(names[:1])
        inclusive.update(set(names))
    return {"planner_samples":samples,"self_samples":top.most_common(25),
            "inclusive_samples":inclusive.most_common(35)}

def instrument(store,original,label):
    anchor="            TempleNetwork plan = TempleNetwork.plan(layout, inputs);"
    if original.count(anchor)!=1:
        raise RuntimeError("Profiler hook anchor moved")
    replacement="""            TempleNetwork plan;
            try (var recording=new jdk.jfr.Recording()) {
                recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10));
                recording.enable("jdk.NativeMethodSample").withPeriod(java.time.Duration.ofMillis(10));
                recording.start();
                var clock=java.lang.management.ManagementFactory.getThreadMXBean();
                long cpuStart=clock.isCurrentThreadCpuTimeSupported()?clock.getCurrentThreadCpuTime():-1;
                long wallStart=System.nanoTime();
                plan=TempleNetwork.plan(layout,inputs);
                long wall=System.nanoTime()-wallStart;
                long cpu=cpuStart<0?-1:clock.getCurrentThreadCpuTime()-cpuStart;
                TearsOfInk.LOGGER.info("D060_PROFILE wall_ms={} cpu_ms={}",wall/1_000_000,cpu<0?-1:cpu/1_000_000);
                recording.stop();
                recording.dump(java.nio.file.Path.of("..","out","d060","LABEL.jfr"));
            }""".replace("LABEL",label)
    store.write_text(original.replace(anchor,replacement))

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--seed",default="20261118")
    args=parser.parse_args()
    seed=str(int(args.seed))
    d060.setup(seed)
    planner=d060.JAVA/"world/TempleNetwork.java"
    store=d060.JAVA/"world/TemplePlanStore.java"
    current=planner.read_bytes()
    original_store=store.read_text()
    old=subprocess.check_output(["git","show",BASELINE+":"+str(planner.relative_to(d060.ROOT))],cwd=d060.ROOT)
    props=d060.RUN/"server.properties"
    original_props=props.read_text()
    result={}
    for label,source in [("baseline",old),("cache",current)]:
        world="toi-d060-profile-"+label
        world_path=d060.RUN/world
        if world_path.exists():
            raise RuntimeError("Refusing to reuse a profile world")
        props.write_text(original_props.replace("level-name=toi-d060-disposable","level-name="+world))
        planner.write_bytes(source)
        instrument(store,original_store,label)
        report=d060.session(label,False)
        log=(d060.OUT/(label+".log")).read_text()
        times=[line.split("D060_PROFILE ",1)[1] for line in log.splitlines() if "D060_PROFILE " in line]
        if len(times)!=1:
            raise RuntimeError("Expected exactly one profiled plan: "+label)
        result[label]={"pass":report["pass"],"seed":report["seed"],"temples":report["temples"],
            "plan_sha256":d060.digest(world_path/"data/tearsofink_temple_network_v3.bin"),
            "adit_sites":report.get("adits",{}).get("sites"),"timing":times[0],
            "profile":summarize(d060.OUT/(label+".jfr"))}
    result["same_plan"]=result["baseline"]["plan_sha256"]==result["cache"]["plan_sha256"]
    result["same_adits"]=result["baseline"]["adit_sites"]==result["cache"]["adit_sites"]
    (d060.OUT/"paired-profile.json").write_text(json.dumps(result,indent=2))
    print("D060_PAIRED_PROFILE "+json.dumps(result),flush=True)
    if not (result["same_plan"] and result["same_adits"] and all(
        result[x]["pass"] and result[x]["seed"]==seed and result[x]["temples"]==13
        and result[x]["profile"]["planner_samples"]>0 for x in ("baseline","cache"))):
        raise SystemExit("D060 paired profile failed or changed authoritative plan")

if __name__=="__main__":
    main()
