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
assert 'layout.roadNear' in source and 'layout.nearLake' in source and 'layout.nearHerald' in source
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
assert 'fieldReady(level, field)' in installation
assert 'provenBattlefieldCorridor(level, field, slot.x(), slot.z())' in installation
assert 'BattlefieldSitePreparation.prepare(level, field, slot)' in installation
assert 'toi_battlefield_installations_v3' in installation
assert 'public int scar(int px, int pz)' in source
terrain = read(base + "world/battlefield/BattlefieldSitePreparation.java")
assert 'preflight' in terrain.lower()
assert 'provenBattlefieldCorridor(level, field, px, pz)' in terrain
assert 'nativeFloor(' in terrain and 'natural(' in terrain
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
# The first 27 anchors are stable. New late-game episodes append strictly
# AFTER the original field and relic beds, without shifting saved scene IDs.
first_27 = source.split('relicBeds(field, placements)', 1)[0]
assert first_27.count('scene(placements, field') == 19
assert source.count('scene(placements, field') == 22
assert source.index('projectileBeds(field, placements)') > source.index('relicBeds(field, placements)')
assert source.rindex('scene(placements, field') > source.index('projectileBeds(field, placements)')
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
assert 'BattlefieldInstallations.fieldReady(level, field)' in discovery
print("D-135: validated deterministic field layout, 27 anchors, safe stamps, roads, temple and fog wiring.")


# D-137: the first 27 scene IDs remain in place. Dense relic beds append after
# those slots and render many actual aged meshes per inert anchor.
assert 'Kind.RELIC' in source and 'relicBeds(field, placements)' in source
assert source.index('relicBeds(field, placements)') > source.index('traces.length')
assert 'BattlefieldAccessRoutes.nearCorridor(field, x, z, 6)' in source
assert 'BattlefieldRelicPlacement.place(level, rootXZ' in installation
assert 'slot.kind() == BattlefieldWorldPlan.Kind.RELIC' in installation
relic_recipes = read(base + "world/battlefield/BattlefieldRelicRecipes.java")
relic_placement = read(base + "world/battlefield/BattlefieldRelicPlacement.java")
relic_entity = read(base + "block/BattlefieldRelicPatchBlockEntity.java")
relic_renderer = read("src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldRelicPatchRenderer.java")
mod_blocks = read(base + "block/ModBlocks.java")
mod_entities = read(base + "block/lantern/ModBlockEntities.java")
assert 'battlefield_relic_patch' in mod_blocks and 'BATTLEFIELD_RELIC_PATCH' in mod_entities
assert 'BATTLEFIELD_RELIC_PATCH' in ui
assert 'BattlefieldRelicRecipes.generate(base, style, variant)' in relic_placement
assert 'getChunkNow' in relic_placement
assert 'public java.util.List<' in relic_entity and 'cached' in relic_entity
assert 'entity.poses()' in relic_renderer and 'BATTLEFIELD_WEAPON_MESH' in relic_renderer
assert 'BattlefieldSoulShieldCatalog.SCALE' in relic_renderer
assert 'case 2 -> 16 +' in relic_recipes
assert 'case 0 -> 14 +' in relic_recipes

# D-137: the inner battlefield sheds almost all fog particles by THEIR own
# position; forest outside retains original particle density.
bank_spawner = read("src/client/java/com/tarkrakov/tearsofink/client/fog/FogBankSpawner.java")
bank_particle = read("src/client/java/com/tarkrakov/tearsofink/client/particle/FogBankParticle.java")
assert 'particleMultiplier(x + .5, z + .5)' in bank_spawner
assert 'particleMultiplier(this.x, this.z)' in bank_particle
assert 'wall ? 1f' in bank_particle
assert 'return 1f - 0.97f * openness' in clarity
assert 'public static float opennessAt' in clarity

# Native cleanup is deliberately item-whitelisted, does not sweep normal drops.
cleanup = read(base + "world/battlefield/BattlefieldGroundCleanup.java")
assert 'nativeDrop(ItemEntity item)' in cleanup
assert 'nativePlant(BlockState s)' in cleanup
assert 'Block.UPDATE_SUPPRESS_DROPS' in cleanup
assert 'CleanedChunks' in installation and 'BattlefieldGroundCleanup.cleanChunk' in installation
assert 'Block.UPDATE_SUPPRESS_DROPS' in environment
assert 'Block.UPDATE_SUPPRESS_DROPS' in terrain

# Connected terrain: all 8 route links join the roadward entry to the main
# field and far upper bank without rerolling historical scene positions.
access = read(base + "world/battlefield/BattlefieldAccessRoutes.java")
match = re.search(r"private static final int\[\]\[\] LINKS = \{(.*?)\};", access, re.S)
assert match is not None
links = []
for line in re.findall(r"\{\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\}", match.group(1)):
    a,b,c,d = map(int, line)
    links.append(((a,b),(c,d)))
assert len(links) == 10
seen = {(-74,0)}
for _ in range(len(links)):
    for a,b in links:
        if a in seen: seen.add(b)
        if b in seen: seen.add(a)
assert all(a in seen and b in seen for a,b in links)
assert (44,44) in seen and (-52,-23) in seen and (61,18) in seen
assert 'JOINED' not in access  # NBT state is owned by the SavedData installer
assert 'JoinedTerrain' in installation and 'BattlefieldAccessRoutes.prepare' in installation
assert 'provenBattlefieldCorridor' in access
assert 'Math.abs(player.getX() - x) < 12' in access
assert 'layout.roadNear' in access and 'temples.overlapsPath' in access
assert 'Block.UPDATE_SUPPRESS_DROPS' in access
assert 'связующие переходы' in world_commands

print("D-137: append-only dense weapon beds, local fog-particle suppression, debris cleanup, 10 connected terrain links.")

# D-138: impact beds reuse the existing inert relic-patch renderer, with
# texture reuse and no per-arrow / per-bolt entities, and saved bare rigs
# default to upright while new scenes use angled states.
assert 'case 5 -> 17 +' in relic_recipes and 'case 6 -> 14 +' in relic_recipes
assert 'case 7 -> 19 +' in relic_recipes and 'public boolean projectile()' in relic_recipes
assert 'drawProjectile(' in relic_renderer and 'textures/entity/projectiles/pale_bolt.png' in relic_renderer
assert 'textures/entity/projectiles/arrow.png' in relic_renderer
assert 'new Placement(placements.size(), Kind.RELIC' in source
banner_block = read(base + "block/BattlefieldBannerBlock.java")
banner_renderer = read("src/client/java/com/tarkrakov/tearsofink/client/battlefield/BattlefieldBannerRenderer.java")
banner_preview = read(base + "command/BattlefieldZonePreview.java")
assert 'IntegerProperty.create("lean", 0, 7)' in banner_block
assert '.setValue(LEAN, 0)' in banner_block
assert 'if (bare && lean != 0)' in banner_renderer
assert 'tiltedPoleFits(' in banner_preview
assert '.setValue(BattlefieldBannerBlock.LEAN, lean)' in banner_preview
assert 'configureGroundMist()' in bank_particle
assert 'renderGroundMist(' in bank_particle and 'getU0()' in bank_particle
assert 'BattlefieldClarity.opennessAt(' in bank_spawner
assert 'shape.max(Direction.Axis.Y) + .035d' in bank_spawner
print("D-138: append-only bolt/arrow patches, tilted bare flags, thin client ground fog.")

# D-140: the previous three scene-based fallen standards were hard to place
# and can be falsely counted if an unrelated adjacent scene exists. The new
# decoration is independent, faction-authentic, heavily tilted and actually
# rendered as several buried meshes per ONE inert existing relic BE.
assert 'barePoleBeds(field, placements)' in source
assert source.index('barePoleBeds(field, placements)') > source.rindex('scene(placements, field')
assert source.index('barePoleBeds(field, placements)') > source.index('projectileBeds(field, placements)')
assert 'slot.blade() >= 8' in installation
assert 'hasBarePoleAnchor(level, slot)' in installation
assert 'slot.blade() < 8' in installation # no new mass-earthworks for poles
assert 'style >= 8 ? 2 :' in relic_placement
assert 'barePoleFits(' in relic_placement
assert 'case 14 -> 6.125f;' in relic_recipes
assert 'case 15 -> 6.0f;' in relic_recipes
assert 'case 0 -> .18f;' in relic_recipes
assert 'case 1 -> .50f;' in relic_recipes
assert 'case 2 -> .73f;' in relic_recipes
assert 'default -> .88f;' in relic_recipes
assert 'public boolean pole()' in relic_recipes
assert 'p.pole()' in relic_renderer
assert 'BattlefieldBannerRenderer.renderBareRelic' in relic_renderer
assert 'renderBareRelic(int mesh' in banner_renderer
assert 'BattlefieldFactionBannerGeometry.model(faction, true)' in banner_renderer
assert 'POLE_PARTS' in banner_renderer
assert 'IntegerProperty.create("style", 0, 10)' in read(base + "block/BattlefieldRelicPatchBlock.java")
assert 'вкопанные древки' in world_commands
print("D-140: append-only inert buried author-pole groups, safe terrain and exact faction rigs.")
