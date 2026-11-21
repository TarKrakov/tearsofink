package com.tarkrakov.tearsofink.hunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Тестовая Охота рядом с собой с заказанным охотником:
 * <pre>/tearsofink hunt test &lt;тип&gt; &lt;ранг&gt; &lt;оружие&gt; [второе|none] [цель|random] [особенность]</pre>
 * тип — spire | blood | ink; ранг — 0 (Новичок), 1 (Опытный), 2 (Легендарный);
 * оружие — имя предмета мода (blood_talvar, blood_dussak, spire_torturer_greatsword, spire_shadow_scimitar…);
 * цель — имя существа из hunt_targets (zombie, pale_spearman…); особенность — none | swift | giant | split.
 * Идущая мировая Охота останавливается.
 */
public final class HuntTestCommand {
	private HuntTestCommand() {}

	private static final SuggestionProvider<CommandSourceStack> TYPES = (c, b) ->
			SharedSuggestionProvider.suggest(List.of("spire", "blood", "ink"), b);
	private static final SuggestionProvider<CommandSourceStack> WEAPONS = (c, b) -> SharedSuggestionProvider.suggest(weapons(false), b);
	private static final SuggestionProvider<CommandSourceStack> OFF = (c, b) -> SharedSuggestionProvider.suggest(weapons(true), b);
	private static final SuggestionProvider<CommandSourceStack> TARGETS = (c, b) -> {
		List<String> out = new ArrayList<>();
		out.add("random");
		for (HuntTargets.Target t : HuntTargets.all()) out.add(BuiltInRegistries.ENTITY_TYPE.getKey(t.type()).getPath());
		return SharedSuggestionProvider.suggest(out, b);
	};
	private static final SuggestionProvider<CommandSourceStack> PREFIXES = (c, b) -> {
		List<String> out = new ArrayList<>();
		for (PreyPrefix p : PreyPrefix.values()) out.add(p.id);
		return SharedSuggestionProvider.suggest(out, b);
	};

	/** Оружие мода, которым может драться охотник (клинки архетипов и оружие охотников). */
	private static List<String> weapons(boolean withNone) {
		List<String> out = new ArrayList<>();
		if (withNone) out.add("none");
		for (Item item : BuiltInRegistries.ITEM) {
			var id = BuiltInRegistries.ITEM.getKey(item);
			if (!id.getNamespace().equals(TearsOfInk.MOD_ID)) continue;
			if (item instanceof com.tarkrakov.tearsofink.weapon.InkGreatswordItem
					&& !LegacyHunterWeapons.isRetired(item)) out.add(id.getPath());
		}
		return out;
	}

	public static ArgumentBuilder<CommandSourceStack, ?> build() {
		return Commands.literal("test")
				.then(Commands.argument("type", StringArgumentType.word()).suggests(TYPES)
						.then(Commands.argument("rank", IntegerArgumentType.integer(0, 2))
								.then(Commands.argument("main", StringArgumentType.word()).suggests(WEAPONS)
										.executes(c -> run(c, false, false, false))
										.then(Commands.argument("off", StringArgumentType.word()).suggests(OFF)
												.executes(c -> run(c, true, false, false))
												.then(Commands.argument("target", StringArgumentType.word()).suggests(TARGETS)
														.executes(c -> run(c, true, true, false))
														.then(Commands.argument("prefix", StringArgumentType.word()).suggests(PREFIXES)
																.executes(c -> run(c, true, true, true))))))));
	}

	private static int run(CommandContext<CommandSourceStack> c, boolean hasOff, boolean hasTarget, boolean hasPrefix) {
		CommandSourceStack src = c.getSource();
		ServerPlayer p = src.getPlayer();
		ServerLevel lands = ModDimensions.lands(src.getServer());
		if (p == null || lands == null || p.level() != lands) {
			src.sendFailure(Component.literal("Тестовая Охота — только в Землях, рядом с вами."));
			return 0;
		}
		HunterType type = null;
		String ts = StringArgumentType.getString(c, "type");
		for (HunterType t : HunterType.values()) if (t.id.equals(ts)) type = t;
		if (type == null) {
			src.sendFailure(Component.literal("Тип: spire, blood или ink."));
			return 0;
		}
		int rank = IntegerArgumentType.getInteger(c, "rank");
		ItemStack main = weapon(StringArgumentType.getString(c, "main"), type, rank);
		if (main == null || main.isEmpty()) {
			src.sendFailure(Component.literal("Нет такого оружия: " + StringArgumentType.getString(c, "main")));
			return 0;
		}
		ItemStack off = ItemStack.EMPTY;
		if (hasOff && !StringArgumentType.getString(c, "off").equals("none")) {
			off = weapon(StringArgumentType.getString(c, "off"), type, rank);
			if (off == null) {
				src.sendFailure(Component.literal("Нет такого оружия: " + StringArgumentType.getString(c, "off")));
				return 0;
			}
		}
		HuntTargets.Target target = null;
		if (hasTarget && !StringArgumentType.getString(c, "target").equals("random")) {
			String want = StringArgumentType.getString(c, "target");
			for (HuntTargets.Target t : HuntTargets.all()) {
				if (BuiltInRegistries.ENTITY_TYPE.getKey(t.type()).getPath().equals(want)) target = t;
			}
			if (target == null) {
				src.sendFailure(Component.literal("Нет такой цели в hunt_targets: " + want));
				return 0;
			}
		}
		PreyPrefix prefix = null;
		if (hasPrefix) {
			String want = StringArgumentType.getString(c, "prefix");
			for (PreyPrefix pp : PreyPrefix.values()) if (pp.id.equals(want)) prefix = pp;
			if (prefix == null) {
				src.sendFailure(Component.literal("Особенность: none, swift, giant или split."));
				return 0;
			}
		}
		if (Hunt.running()) Hunt.stop(lands);
		boolean ok = Hunt.start(lands, p, new Hunt.Spec(type, rank, main, off, target, prefix));
		if (!ok) src.sendFailure(Component.literal("Не нашлось места для цели — отойдите от храма или воды."));
		return ok ? 1 : 0;
	}

	@Nullable
	private static ItemStack weapon(String name, HunterType type, int rank) {
		var id = name.contains(":") ? net.minecraft.resources.ResourceLocation.tryParse(name) : TearsOfInk.id(name);
		if (id == null) return null;
		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == Items.AIR || !(item instanceof com.tarkrakov.tearsofink.weapon.InkGreatswordItem)
				|| LegacyHunterWeapons.isRetired(item)) return null;
		ItemStack stack = new ItemStack(item);
		com.tarkrakov.tearsofink.weapon.forge.Gear.setTier(stack, rank);
		return stack;
	}
}
