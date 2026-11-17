package com.tarkrakov.tearsofink.client.combat;

import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** Клинки на стороне игрока: ЛКМ → просьба об ударе, движения от сервера, доставание при смене оружия. */
public final class CombatClient {
	/** Сколько тиков держать ЛКМ, чтобы клинок начал копать (клик по блоку — просто удар). */
	public static final int MINING_HOLD = 6;
	private static int attackHeld;
	private static long lastRequest;

	public static int attackHeld() {
		return attackHeld;
	}
	private static ItemStack lastMain = ItemStack.EMPTY, lastOff = ItemStack.EMPTY;
	private static int lastSlot = -1;

	private CombatClient() {}

	public static void init() {
		WeaponAnims.init();
		// Локальный просмотр авторских тестов: не вызывает серверный боевой приём.
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("toianimtest")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("single")
								.executes(ctx -> playBlockbenchTest(ctx.getSource(), false)))
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("dual")
								.executes(ctx -> playBlockbenchTest(ctx.getSource(), true)))));
		ClientPlayNetworking.registerGlobalReceiver(Combat.Anim.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			var player = ctx.client().player;
			var level = ctx.client().level;
			if (player == null || level == null) return;
			Archetype a = Archetype.byId(payload.archetype());
			if (a == null) return;
			String anim = payload.anim();
			boolean offTag = anim.startsWith("off:"); // парные приёмы: движение второй руки
			if (offTag) anim = anim.substring(4);
			// третье лицо — у всех (и у себя в F5)
			if (level.getEntity(payload.entity()) instanceof net.minecraft.world.entity.LivingEntity who && !anim.isEmpty()) {
				var side = offTag ? who.getMainArm().getOpposite() : who.getMainArm();
				ThirdPerson.play(who.getId(), side, a, anim, payload.tempo());
			}
			if (payload.entity() != player.getId()) return;
			var arm = offTag ? player.getMainArm().getOpposite() : player.getMainArm();
			boolean off = arm != player.getMainArm();
			// пустое движение — в этом ударе рука не двигается (но шкала у правой всё равно сбрасывается)
			if (!anim.isEmpty()) WeaponAnims.play(arm, a, anim, off ? 0 : payload.ticks(), payload.tempo());
			if (off) return;
			if (payload.speed() >= 0) { // удар связки (не приём): шкала под прицелом — как у сервера
				player.resetAttackStrengthTicker();
				fillSpeed = payload.speed();
				fillCarry = 0;
			}
		}));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ThirdPerson.clear());
		ClientTickEvents.END_CLIENT_TICK.register(CombatClient::tick);
	}

	/** Только визуальное воспроизведение из сохранённой сцены Blockbench, без урона и серверных событий. */
	private static int playBlockbenchTest(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, boolean dual) {
		var player = Minecraft.getInstance().player;
		if (player == null) return 0;
		Archetype archetype = Archetype.PALE_LINEAR_GREATSWORD;
		if (Archetype.of(player.getMainHandItem()) != archetype
				|| (dual && Archetype.of(player.getOffhandItem()) != archetype)) {
			source.sendError(net.minecraft.network.chat.Component.literal(
					dual ? "Для теста dual возьми Бледный линейный двуручник в обе руки."
						: "Для теста single возьми Бледный линейный двуручник в главную руку."));
			return 0;
		}
		String name = dual ? "bb_test_dual" : "bb_test_single";
		if (!WeaponAnims.has(archetype, name) || (dual && !WeaponAnims.has(archetype, name + "_off"))) {
			source.sendError(net.minecraft.network.chat.Component.literal(
					"Тестовая анимация не загружена. Проверь версию мода и нажми F3+T."));
			return 0;
		}
		HumanoidArm main = player.getMainArm();
		WeaponAnims.play(main, archetype, name, 0, 1f);
		ThirdPerson.play(player.getId(), main, archetype, name, 1f);
		if (dual) {
			HumanoidArm off = main.getOpposite();
			WeaponAnims.play(off, archetype, name + "_off", 0, 1f);
			ThirdPerson.play(player.getId(), off, archetype, name + "_off", 1f);
		}
		source.sendFeedback(net.minecraft.network.chat.Component.literal(
				"Blockbench: " + name + " (1 секунда; визуальный тест, без урона)."));
		return 1;
	}

	/** ЛКМ с клинком архетипа. Решает сервер: чуть раньше времени — удар запоминается и выходит вовремя. */
	public static void requestSwing() {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		long now = System.currentTimeMillis();
		if (now - lastRequest < 120) return;
		lastRequest = now;
		equipping = false;
		ClientPlayNetworking.send(new Combat.Swing());
	}

	private static boolean equipping;
	private static float fillSpeed = 1f, fillCarry;

	private static void tick(Minecraft mc) {
		var player = mc.player;
		if (player == null) {
			lastMain = ItemStack.EMPTY;
			lastOff = ItemStack.EMPTY;
			lastSlot = -1;
			return;
		}
		attackHeld = mc.options.keyAttack.isDown() && mc.screen == null ? attackHeld + 1 : 0;
		if (fillSpeed != 1f) {
			fillCarry = Combat.fastFill(player, fillSpeed, fillCarry);
			if (player.getAttackStrengthScale(0) >= 1f) fillSpeed = 1f;
		}
		// копаем зажатой ЛКМ — движение удара прерывается, рука машет по-ванильному
		if (mc.gameMode != null && mc.gameMode.isDestroying()) WeaponAnims.stop(player.getMainArm());
		ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
		int slot = player.getInventory().selected;
		if (slot != lastSlot || !ItemStack.isSameItem(main, lastMain)) equip(player.getMainArm(), main);
		if (!ItemStack.isSameItem(off, lastOff)) equip(player.getMainArm().getOpposite(), off);
		lastMain = main.copy();
		lastOff = off.copy();
		lastSlot = slot;
	}

	private static void equip(HumanoidArm arm, ItemStack stack) {
		Archetype a = Archetype.of(stack);
		if (a != null && WeaponAnims.has(a, "equip")) {
			WeaponAnims.play(arm, a, "equip", 0, 1f);
			if (arm == Minecraft.getInstance().player.getMainArm()) equipping = true;
		} else {
			WeaponAnims.stop(arm);
		}
	}
}
