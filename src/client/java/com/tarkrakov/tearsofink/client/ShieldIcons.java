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
	private static final Map<Item, String> ICONS = Map.of(
			ModItems.RAM_SHIELD, "ram_shield", ModItems.RADIANT_SHIELD, "radiant_shield",
			ModItems.MERCY_SHIELD, "mercy_shield", ModItems.REPEL_SHIELD, "repel_shield",
			ModItems.WAVE_GREATSWORD, "wave_greatsword", ModItems.CLEAVE_GREATSWORD, "cleave_greatsword",
			ModItems.INK_LONGKNIFE, "ink_longknife");

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
