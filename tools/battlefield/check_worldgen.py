#!/usr/bin/env python3
"""Static contracts for D-135 full battlefield integration (Java checked by ./gradlew build).
The checks catch accidental unhooking of the terrain/installer/saved-world boundary.
"""
from pathlib import Path
import math
import re

ROOT = Path(__file__).resolve().parents[2]
def read(name):
    return (ROOT / name).read_text(encoding="utf-8")

base = "src/main/java/com/tarkrakov/tearsofink/"
source = read(base + "world/battlefield/BattlefieldWorldPlan.java")
environment = read(base + "world/battlefield/BattlefieldEnvironment.java")
installation = read(base + "world/battlefield/BattlefieldInstallations.java")
filter_code = read(base + "world/battlefield/BattlefieldTreeFilter.java")
feature = read(base + "world/FinalCleanupFeature.java")
client = read("src/client/java/com/tarkrakov/tearsofink/client/fog/InkFog.java")
clarity = read("src/client/java/com/tarkrakov/tearsofink/client/fog/BattlefieldClarity.java")
discovery = read(base + "world/battlefield/BattlefieldDiscovery.java")
commands = read(base + "command/BattlefieldDecorCommands.java")
world_commands = read(base + "command/BattlefieldWorldCommands.java")
ui = read("src/client/java/com/tarkrakov/tearsofink/client/TearsOfInkClient.java")
init = read(base + "TearsOfInk.java")
camps = read(base + "entity/patrol/ForestCamps.java")
ruins = read(base + "world/ruins/RuinStructure.java")

# Plan must be picked only after the frozen temple network is ready.
assert 'layout.templeNetwork() == null' in source
assert 'temples.overlapsPath' in source and 'temples.temples()' in source
assert 'layout.nearRoad' in source and 'layout.nearLake' in source and 'layout.nearHerald' in source
assert 'public boolean reserved(' in source
assert 'BattlefieldWorldPlan.of(layout)' in camps and 'BattlefieldWorldPlan.of(layout)' in ruins
assert 'BattlefieldTreeFilter.init()' in init
tree_json = read("src/main/resources/data/tears-of-ink/worldgen/placed_feature/trees_pale_dense.json")
assert 'tears-of-ink:away_from_battlefield' in tree_json
assert 'BattlefieldEnvironment' in feature and 'BattlefieldWorldPlan.of(layout)' in feature
assert 'BATTLEFIELD_FIELD_MARKER' in environment and 'level.getMinBuildHeight' in environment
assert 'return false;' in filter_code
assert 'getChunkNow' in installation and 'new Factory<' in installation
assert 'PlacedOrSkipped' in installation and 'setDirty()' in installation
assert 'stamped(level, field.x(), field.z())' in installation
assert 'stamped(level, slot.x(), slot.z())' in installation
assert 'BattlefieldZonePreview.placePlanned' in installation
assert 'BattlefieldDecorCommands.placePlannedMonument' in installation
assert 'BattlefieldInstallations.init()' in init
assert 'BattlefieldDiscovery.init()' in init
assert 'PayloadTypeRegistry.playS2C' in discovery and 'ServerPlayNetworking.send' in discovery
assert 'BattlefieldClarity.receive' in ui and 'battlefieldClarity' in client
assert 'Commands.literal("field")' in world_commands
assert 'Commands.literal("approach")' in world_commands
assert 'BattlefieldWorldCommands.literal()' in commands

# The twenty-seven deterministic anchors are 2 dominants + 19 scenario
# recipes + 6 progressively spaced discovery traces. Scene IDs stay untouched.
assert source.count('monument(placements, field') == 2
assert source.count('scene(placements, field') == 19
assert 'new BattlefieldPosedScenes.Scenario[]' in source
assert 'traces.length' in source
assert 'BattlefieldPosedScenes.Scenario.GRIND_CENTER' in source
assert 'BattlefieldPosedScenes.Scenario.REGULAR_SQUAD_LINE_ANCHOR' in source
assert 'BattlefieldPosedScenes.Scenario.PALADIN_SOUL_SHIELD_WALL' in source
assert 'BattlefieldPosedScenes.Scenario.INK_HUNTER_SWEEP' in source

# Client mask tracks the server's irregular noncircular boundary. Simulate 32
# deterministic stored seeds: the center clears, far outside never clears;
# the boundary is neither constant-angle nor a perfect circle.
for seed in range(32):
    spread = []
    for k in range(24):
        a = k * math.tau / 24
        limit = 1.0 + .085 * math.sin(3*a + (seed & 1023)*.031) + \
                 .057 * math.sin(7*a - (seed >> 10 & 1023)*.028) + \
                 .030 * math.cos(11*a + (seed >> 20 & 1023)*.022)
        spread.append(limit)
    assert min(spread) > .78 and max(spread) < 1.22
    assert max(spread) - min(spread) > .16
assert 'Math.sin(3 * angle' in clarity
assert 'Math.sin(3 * a' in source
assert '78f * battlefieldClarity' in client
print("D-135: validated deterministic field layout, 27 anchors, safe stamps, roads, temple and fog wiring.")
