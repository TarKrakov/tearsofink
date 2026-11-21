#!/usr/bin/env python3
"""D-107 safeguards: faction loadouts, saved item IDs, retired recipes and clean UI."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/tarkrakov/tearsofink"
hunt = (JAVA / "hunt/Hunt.java").read_text()
entity = (JAVA / "hunt/HunterEntity.java").read_text()
legacy = (JAVA / "hunt/LegacyHunterWeapons.java").read_text()
test = (JAVA / "hunt/HuntTestCommand.java").read_text()
tab = (JAVA / "item/ModCreativeTab.java").read_text()
items = (JAVA / "item/ModItems.java").read_text()
jei = (ROOT / "src/client/java/com/tarkrakov/tearsofink/client/jei/TearsOfInkJei.java").read_text()
renderer = (ROOT / "src/client/java/com/tarkrakov/tearsofink/client/hunt/HunterRenderer.java").read_text()

retired = (
    "HUNTER_GREATSWORD", "HUNTER_SABER", "PALE_SWORD", "PALE_GREATSWORD",
    "BLOOD_SWORD", "BLOOD_GREATSWORD", "INK_SWORD", "INK_GREATSWORD",
    "BLOOD_KATANA", "BLOOD_OBSI_CLUB"
)
active = (
    "WAVE_GREATSWORD", "CLEAVE_GREATSWORD", "ROARING_AXE", "RAPID_KNIFE",
    "BLOOD_TALVAR", "BLOOD_DUSSAK",
    "PALE_MISERICORDE", "PALE_LONGSWORD", "PALE_LINEAR_GREATSWORD",
    "BLOOD_KATAR", "BLOOD_PATA", "BLOOD_CURVED_GREATSWORD",
    "INK_SEAX", "INK_FALCHION", "INK_KRIEGSMESSER"
)
for x in retired:
    assert f"output.accept(ModItems.{x})" not in tab, x
    assert f"new ItemStack(ModItems.{x})" in jei, x
    assert f"item == ModItems.{x}" in legacy, x
    assert x in items, "Legacy registry ID removed: " + x

for x in active:
    assert f"output.accept(ModItems.{x})" in tab, x

assert "if (type == HunterType.BLOOD) return bloodLoadout(rank, random);" in hunt
assert "return spireLoadout(rank, random);" in hunt
assert "HunterWeaponItem kind" not in hunt
assert "ModItems.ROARING_AXE, ModItems.WAVE_GREATSWORD, ModItems.RAPID_KNIFE" in hunt
assert "Gear.setTier(arms[0], rank)" in hunt
assert "Gear.setTier(arms[1], rank)" in hunt
assert "LegacyHunterWeapons.legacyTarget" in hunt
assert "LegacyHunterWeapons.migrateInventory" in hunt
assert "Gear.setTier(realMain, rank)" in entity
assert "LegacyHunterWeapons.isRetired(item)" in test
assert "result.applyComponents(source.getComponentsPatch())" in legacy
assert "ItemInHandLayer" in renderer

recipes = ROOT / "src/main/resources/data/tears-of-ink/recipe"
for x in ("pale_sword", "pale_greatsword", "blood_sword",
          "blood_greatsword", "ink_sword", "ink_greatsword"):
    assert not (recipes / (x + ".json")).exists(), x

for x in ("forge_spire_shadow_scimitar", "forge_spire_torturer_greatsword",
          "forge_spire_roaring_axe", "forge_spire_rapid_knife",
          "forge_blood_talvar", "forge_blood_dussak",
          "pale_misericorde", "pale_longsword", "pale_linear_greatsword",
          "blood_katar", "blood_pata", "blood_curved_greatsword",
          "ink_seax", "ink_falchion", "ink_kriegsmesser"):
    json.loads((recipes / (x + ".json")).read_text())

print("D-107 hunter equipment, old contracts, creative/JEI, and recipes: OK")
