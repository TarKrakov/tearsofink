"""D-106: no stone lantern art in runtime, personal slots and collectible diary remain wired."""
import json
from pathlib import Path

root = Path("src/main/resources")
src = Path("src/main/java/com/tarkrakov/tearsofink")
client = Path("src/client/java/com/tarkrakov/tearsofink")
assets = root / "assets/tears-of-ink"
data = root / "data/tears-of-ink"
reserved = Path("reserved/stone_lantern")

for path in ("stone_lantern.bbmodel", "models/block/stone_lantern.json",
             "textures/block/brzier_rock_main.png", "stone_lantern_recipe.json"):
    assert (reserved / path).is_file(), path
for path in (assets / "models/block/stone_lantern.json",
             assets / "textures/block/brzier_rock_main.png",
             data / "recipe/stone_lantern.json"):
    assert not path.exists(), path

ink = json.loads((data / "recipe/ink_lantern.json").read_text())
assert ink["result"] == {"id":"tears-of-ink:ink_lantern","count":2}
assert sorted(ink["pattern"]) == sorted([" A ","UTI"," A "])
assert {k:v["item"] for k,v in ink["key"].items()} == {
    "A":"tears-of-ink:asphoda", "U":"tears-of-ink:ink",
    "T":"tears-of-ink:tearbloom", "I":"minecraft:iron_ingot"}
assert not any("minecraft:empty" in str(ink) for _ in [0])
lantern = (src / "block/lantern/Lanterns.java").read_text()
assert "PLAYER_MADE_LIMIT" not in lantern
assert "public static final int LIGHT_COST = 10" in lantern
douse = (src / "entity/patrol/DouseLanternGoal.java").read_text()
assert "protectedLantern" in douse and "douseable" in douse
blocks = (src / "block/ModBlocks.java").read_text()
assert 'register("stone_lantern"' in blocks, "Keep legacy ID for saves"
creative = (src / "item/ModCreativeTab.java").read_text()
assert "output.accept(ModBlocks.STONE_LANTERN)" not in creative
old_loot = json.loads((data / "loot_table/blocks/stone_lantern.json").read_text())
assert old_loot["pools"][0]["entries"][0]["name"] == "tears-of-ink:ink_lantern"

for mixin, name in ((root / "tears-of-ink.mixins.json", "InventoryMenuMixin"),
                    (Path("src/client/resources/tears-of-ink.client.mixins.json"), "InventoryScreenMixin")):
    manifest = json.loads(mixin.read_text())
    assert name in manifest["mixins"] if name == "InventoryMenuMixin" else name in manifest["client"]
slots = (src / "prologue/PersonalSlots.java").read_text()
assert ".persistent(ItemStack.OPTIONAL_CODEC.listOf())" in slots
assert ".copyOnDeath()" in slots and "migrateStarterItems" in slots
assert all(word in slots for word in ("ModItems.DIARY","ModItems.DIM_MIRROR","ModItems.INK_TEAR","ModItems.SHARD_OF_HOPE"))
slot_mixin = (src / "mixin/SlotMixin.java").read_text()
menu_mixin = (src / "mixin/ContainerMenuMixin.java").read_text()
assert "instanceof PersonalSlots.Items" in slot_mixin and "instanceof PersonalSlots.Items" in menu_mixin
diary = (client / "prologue/DiaryScreen.java").read_text()
section = (client / "prologue/DiarySection.java").read_text()
assert "DiarySection.COLLECTION" in diary and 'COLLECTION("collection"' in section
prologue = (src / "prologue/Prologue.java").read_text()
assert "DiaryEntries.COLLECTION_INVITATION" in prologue
for lang in ("ru_ru","en_us"):
    loc = json.loads((assets / f"lang/{lang}.json").read_text())
    for key in ("gui.tears-of-ink.personal", "diary.tears-of-ink.section.collection",
                "diary.tears-of-ink.entry.collection_invitation.title"):
        assert key in loc, key

print("D-106: personal slots, collection and reserved author art verified")
