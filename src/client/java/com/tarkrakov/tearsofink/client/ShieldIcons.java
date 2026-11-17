package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.effect.ModEffects;
import com.tarkrakov.tearsofink.item.ModItems;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Щиты Души и клинки: в руке — 3D-модель (щиты — models/item/shield/*.json, клинки — models/item/<предмет>.json),
 * в инвентаре — плоский рисунок автора (models/item/icon/*.json).
 * Подмену в окнах делает ItemRendererMixin.
 */
public final class ShieldIcons {
	private static final Map<Item, String> ICONS = Map.ofEntries(
			Map.entry(ModItems.RAM_SHIELD, "ram_shield"), Map.entry(ModItems.RADIANT_SHIELD, "radiant_shield"),
			Map.entry(ModItems.MERCY_SHIELD, "mercy_shield"), Map.entry(ModItems.REPEL_SHIELD, "repel_shield"),
			Map.entry(ModItems.WORN_RAM_SHIELD, "worn_ram_shield"), Map.entry(ModItems.WORN_MERCY_SHIELD, "worn_mercy_shield"),
			Map.entry(ModItems.DIM_SCIMITAR, "dim_scimitar"),
			Map.entry(ModItems.PALE_SWORD, "pale_sword"), Map.entry(ModItems.BLOOD_SWORD, "blood_sword"), Map.entry(ModItems.INK_SWORD, "ink_sword"),
			Map.entry(ModItems.PALE_GREATSWORD, "pale_greatsword"),
			Map.entry(ModItems.PALE_LINEAR_GREATSWORD, "pale_linear_greatsword"), Map.entry(ModItems.BLOOD_GREATSWORD, "blood_greatsword"),
			Map.entry(ModItems.INK_GREATSWORD, "ink_greatsword"),
			Map.entry(ModItems.WAVE_GREATSWORD, "spire_shadow_scimitar"), Map.entry(ModItems.CLEAVE_GREATSWORD, "spire_torturer_greatsword"),
			Map.entry(ModItems.INK_LONGKNIFE, "ink_longknife"),
			Map.entry(ModItems.ROARING_AXE, "spire_roaring_axe"),
			Map.entry(ModItems.RAPID_KNIFE, "spire_rapid_knife"),
			Map.entry(ModItems.BLOOD_KATANA, "blood_katana"),
			Map.entry(ModItems.BLOOD_OBSI_CLUB, "blood_obsi_club"),
			Map.entry(ModItems.BLOOD_TALVAR, "blood_talvar"),
			Map.entry(ModItems.BLOOD_DUSSAK, "blood_dussak"));

	private ShieldIcons() {}

	private static ResourceLocation icon(String name) {
		return TearsOfInk.id("item/icon/" + name);
	}

	public static void init() {
		ModelLoadingPlugin.register(ctx -> {
			for (String name : ICONS.values()) ctx.addModels(icon(name));
			for (String kind : new String[]{"hunter_greatsword", "hunter_saber"}) {
				for (var line : com.tarkrakov.tearsofink.hunt.HunterType.values()) ctx.addModels(icon(kind + "_" + line.id));
			}
			ctx.addModels(icon("radiant_shield_glow"));
		});
	}

	/** Плоская иконка для окна или null, если предмет — не наш щит. */
	public static BakedModel iconFor(ItemStack stack) {
		String name = ICONS.get(stack.getItem());
		if (stack.getItem() instanceof com.tarkrakov.tearsofink.hunt.HunterWeaponItem) { // оружие охотников — рисунок своей линии
			name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
					+ "_" + com.tarkrakov.tearsofink.hunt.HunterWeaponItem.line(stack).id;
		}
		if (name == null) return null;
		Minecraft mc = Minecraft.getInstance();
		if (stack.is(ModItems.RADIANT_SHIELD) && mc.player != null && mc.player.hasEffect(ModEffects.RADIANCE)) name = "radiant_shield_glow";
		return mc.getModelManager().getModel(icon(name));
	}
}
