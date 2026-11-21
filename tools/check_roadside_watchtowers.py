#!/usr/bin/env python3
"""D-108: verify road-adjacent watchtower slots without changing the road generator."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
layout = (root / "src/main/java/com/tarkrakov/tearsofink/world/InkLandsLayout.java").read_text()
camps = (root / "src/main/java/com/tarkrakov/tearsofink/entity/patrol/ForestCamps.java").read_text()
outposts = (root / "src/main/java/com/tarkrakov/tearsofink/entity/patrol/Outposts.java").read_text()

def integer(name):
    return int(re.search(r"public static final int " + name + r"\s*=\s*(\d+)", layout).group(1))

radius, core = integer("RADIUS"), integer("CORE_RADIUS")
bridge = integer("BROKEN_BRIDGE_LEN")
end = core - 4
slots = [float(v) for v in re.search(r"OUTPOSTS = \{([^}]+)\}", layout).group(1).split(",")]
assert len(slots) == 2 and slots[0] < slots[1]
inner, outer = [end + bridge + f * (radius - end - bridge) for f in slots]
first, second = (radius + outer) / 2, (outer + inner) / 2
assert outer + 100 < first < radius - 100
assert inner + 100 < second < outer - 100

assert "return (InkLandsLayout.RADIUS+layout.outpostDistance(1))*.5;" in camps
assert "return (layout.outpostDistance(1)+layout.outpostDistance(0))*.5;" in camps
assert "for(int section=0;section<2;section++)" in camps
assert "ROAD_TOWER_NAMESPACE=0x3000000000000000L" in camps
assert "ROAD_TOWER_ALONG_OFFSETS={0,-32,32,-64,64,-96,96}" in camps
assert "ROAD_TOWER_SIDE_OFFSETS={24,29}" in camps
assert "for(double shift:ROAD_TOWER_ALONG_OFFSETS)" in camps
assert all(inner + 100 < second + shift < outer - 100 for shift in (0,-32,32,-64,64,-96,96))
assert all(outer + 100 < first + shift < radius - 60 for shift in (0,-32,32,-64,64,-96,96))
assert "Camp c=install(level,layout,k,0," in camps  # authored tower type only
assert "data.install(level,layout,k,type,rot,site,false)" in camps  # original grid unchanged
assert "if(roadside&&layout.nearRoad(x,z,7))return null;" in camps
assert "if(!ModDimensions.chunksReady(level,site,20))" not in camps  # combined guard instead
assert "||!ModDimensions.chunksReady(level,site,20)" in camps
assert "||Ruins.nearStart(layout,x,z)||Sanctuary.near(layout,x,z,48)" in camps
assert "||near(level,site,MIN_CAMP_GAP)||BarricadeGates.nearGate(level,site,64)" in camps
assert "ForestCamps.tick(level,layout,canSpawn)" in outposts
assert camps.index("if(roadside&&layout.nearRoad(x,z,7))return null;") < camps.index("changes.put(p,ModBlocks.ASHEN_SOIL.defaultBlockState());")
assert camps.count("return spireLoadout") == 0  # world placement never touches hunter equipment
print(f"D-108: roadside watchtower slots at d={first:.1f} and d={second:.1f}, "
      f"outposts at d={outer:.1f} / d={inner:.1f}: OK")
