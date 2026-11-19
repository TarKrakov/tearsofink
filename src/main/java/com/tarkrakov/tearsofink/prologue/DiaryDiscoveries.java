package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.entity.PaleOwlEntity;
import com.tarkrakov.tearsofink.entity.PaleDeerEntity;
import com.tarkrakov.tearsofink.entity.PaleStriderEntity;
import com.tarkrakov.tearsofink.hunt.HarvestKeeperEntity;
import com.tarkrakov.tearsofink.hunt.HunterEntity;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.item.OrdinaryArmorItem;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.adit.AditStructure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Temporary author-replaceable pages. Discovery stays server-side and in the existing diary attachment. */
final class DiaryDiscoveries {
	private static final List<String> OBSERVATIONS = List.of(
			"draft_pale_owl", "draft_pale_deer", "draft_pale_strider", "draft_soul_bargain", "draft_harvest",
			"draft_hunt_field", "draft_hunter_spire", "draft_hunter_blood", "draft_hunter_ink", "draft_mad_order");
	private static final List<String> ITEMS = List.of("draft_asphoda", "draft_blood_metal",
			"draft_ink_metal", "draft_weaponcraft", "draft_armor", "draft_alchemy", "draft_mad_order", "draft_harvest", "draft_blood_collector");

	private DiaryDiscoveries() {}

	private static boolean missing(List<String> known, List<String> found, String id) {
		return !known.contains(id) && !found.contains(id);
	}

	private static void add(List<String> known, List<String> found, String id) {
		if (missing(known, found, id)) found.add(id);
	}

	static void collect(ServerPlayer p, List<String> known, List<String> found) {
		if (p.isSpectator() || !p.isAlive()) return;
		var vow = com.tarkrakov.tearsofink.vow.Vows.vow(p);
		if (vow == com.tarkrakov.tearsofink.vow.Vow.SOUL) add(known, found, "draft_soul_deeds");
		else if (vow == com.tarkrakov.tearsofink.vow.Vow.INK) add(known, found, "draft_ink_ranks");
		var level = p.serverLevel();
		var pos = p.blockPosition();
		// The player chunk is already loaded; no locate or world generation.
		if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null) {
			var biome = level.getBiome(pos);
			if (biome.is(ModBiomes.PALE_FOREST)) add(known, found, "draft_region_pale");
			else if (biome.is(ModBiomes.BLOODWOOD)) add(known, found, "draft_region_blood");
			else if (biome.is(ModBiomes.INKWOOD)) add(known, found, "draft_region_ink");
			else if (biome.is(ModBiomes.TAR_LAKE)) add(known, found, "draft_region_lake");
			else if (biome.is(ModBiomes.EDGE)) add(known, found, "draft_region_edge");
			if (missing(known, found, "draft_adit") && AditStructure.nearBuiltPassage(level, pos))
				add(known, found, "draft_adit");
		}
		if (missing(known, found, "draft_fountain_travel") && FountainTravel.hasOpenedFountain(p))
			add(known, found, "draft_fountain_travel");

		if (ITEMS.stream().anyMatch(id -> missing(known, found, id))) {
			var inv = p.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (s.isEmpty()) continue;
				if (s.is(ModItems.ASPHODA) || s.is(ModBlocks.ASPHODELITE_ORE.asItem())
						|| s.is(ModBlocks.DEEPSLATE_ASPHODELITE_ORE.asItem()))
					add(known, found, "draft_asphoda");
				if (s.is(ModItems.RAW_BLOOD_METAL) || s.is(ModItems.BLOOD_INGOT)) add(known, found, "draft_blood_metal");
				if (s.is(ModItems.RAW_INK_METAL) || s.is(ModItems.INK_INGOT)) add(known, found, "draft_ink_metal");
				if (s.getItem() instanceof OrdinaryArmorItem) add(known, found, "draft_armor");
				if (s.getItem() instanceof com.tarkrakov.tearsofink.weapon.forge.OrdinaryWeaponItem)
					add(known, found, "draft_weaponcraft");
				if (s.is(ModBlocks.ALCHEMY_TABLE.asItem()) || s.is(ModItems.TEARBLOOM_BREW)
						|| s.is(ModItems.STRONG_TEARBLOOM_BREW)) add(known, found, "draft_alchemy");
				if (s.is(ModItems.HOPE_ESSENCE)) add(known, found, "draft_mad_order");
				if (s.is(ModItems.CRIMSON_GIFT)) add(known, found, "draft_harvest");
				if (s.is(ModItems.BLOOD_CLOT) || s.is(ModItems.BLOOD_CRYSTAL)) add(known, found, "draft_blood_collector");
			}
		}

		// Bounded query of nearby loaded entities, once per existing guidance interval.
		if (OBSERVATIONS.stream().anyMatch(id -> missing(known, found, id))) {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16),
					mob -> mob.isAlive() && (mob instanceof PaleOwlEntity || mob instanceof PaleDeerEntity || mob instanceof PaleStriderEntity || mob instanceof HarvestKeeperEntity
							|| mob instanceof HunterEntity || mob instanceof SoulTraderEntity))) {
				if (p.distanceToSqr(e) <= 256.0 && p.hasLineOfSight(e)) observe(e, known, found);
			}
		}
	}

	/** Also catches a creature killed before the next observation tick. */
	static void observe(LivingEntity e, List<String> known, List<String> found) {
		if (e instanceof PaleOwlEntity) add(known, found, "draft_pale_owl");
		else if (e instanceof PaleDeerEntity) add(known, found, "draft_pale_deer");
		else if (e instanceof PaleStriderEntity) add(known, found, "draft_pale_strider");
		else if (e instanceof HarvestKeeperEntity) add(known, found, "draft_harvest");
		else if (e instanceof SoulTraderEntity) add(known, found, "draft_soul_bargain");
		else if (e instanceof HunterEntity hunter) {
			add(known, found, "draft_hunt_field");
			add(known, found, "draft_hunter_" + hunter.hunterType().id);
			if (hunter.mad()) add(known, found, "draft_mad_order");
		}
	}
}
