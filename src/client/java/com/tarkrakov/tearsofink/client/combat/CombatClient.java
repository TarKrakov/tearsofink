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
	private static long lastRequest;
	private static ItemStack lastMain = ItemStack.EMPTY, lastOff = ItemStack.EMPTY;
	private static int lastSlot = -1;

	private CombatClient() {}

	public static void init() {
		WeaponAnims.init();
		ClientPlayNetworking.registerGlobalReceiver(Combat.Anim.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			var player = ctx.client().player;
			if (player == null || payload.entity() != player.getId()) return; // чужие удары — позже (третье лицо)
			Archetype a = Archetype.byId(payload.archetype());
			if (a != null) WeaponAnims.play(player.getMainArm(), a, payload.anim());
		}));
		ClientTickEvents.END_CLIENT_TICK.register(CombatClient::tick);
	}

	/** ЛКМ с клинком архетипа. Пока идёт удар — клики не считаются. */
	public static void requestSwing() {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		if (WeaponAnims.busy(player.getMainArm()) && !equipping) return;
		long now = System.currentTimeMillis();
		if (now - lastRequest < 120) return;
		lastRequest = now;
		equipping = false;
		ClientPlayNetworking.send(new Combat.Swing());
	}

	private static boolean equipping;

	private static void tick(Minecraft mc) {
		var player = mc.player;
		if (player == null) {
			lastMain = ItemStack.EMPTY;
			lastOff = ItemStack.EMPTY;
			lastSlot = -1;
			return;
		}
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
			WeaponAnims.play(arm, a, "equip");
			if (arm == Minecraft.getInstance().player.getMainArm()) equipping = true;
		} else {
			WeaponAnims.stop(arm);
		}
	}
}
