package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.combat.CombatClient;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ЛКМ с клинком архетипа — удар связки (решает сервер), даже если под прицелом блок: клик по блоку не рвёт серию.
 * Копать — только зажав ЛКМ дольше CombatClient.MINING_HOLD тиков (тогда — обычное ванильное копание).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Shadow @Nullable public LocalPlayer player;
	@Shadow @Nullable public HitResult hitResult;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void tearsofink$comboSwing(CallbackInfoReturnable<Boolean> cir) {
		if (com.tarkrakov.tearsofink.weapon.blood.Parry.CLIENT_LOCK) { // стойка Дуссака: никаких ударов, даже щитом
			cir.setReturnValue(false);
			return;
		}
		if (player == null || player.isSpectator() || Archetype.of(player.getMainHandItem()) == null) return;
		if (Archetype.of(player.getMainHandItem()) == Archetype.BASTION && !com.tarkrakov.tearsofink.weapon.combat.Combat.bastion(player)) return;
		CombatClient.requestSwing();
		cir.setReturnValue(false);
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void tearsofink$holdToMine(boolean leftClick, CallbackInfo ci) {
		if (com.tarkrakov.tearsofink.weapon.blood.Parry.CLIENT_LOCK) {
			ci.cancel();
			return;
		}
		if (!leftClick || player == null || Archetype.of(player.getMainHandItem()) == null) return;
		if (Archetype.of(player.getMainHandItem()) == Archetype.BASTION && !com.tarkrakov.tearsofink.weapon.combat.Combat.bastion(player)) return;
		if (CombatClient.attackHeld() < CombatClient.MINING_HOLD) ci.cancel();
	}
}
