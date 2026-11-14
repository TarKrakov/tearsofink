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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ЛКМ с клинком архетипа — удар связки (решает сервер). По блокам — как обычно (ломать). */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Shadow @Nullable public LocalPlayer player;
	@Shadow @Nullable public HitResult hitResult;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void tearsofink$comboSwing(CallbackInfoReturnable<Boolean> cir) {
		if (player == null || player.isSpectator() || Archetype.of(player.getMainHandItem()) == null) return;
		if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) return;
		CombatClient.requestSwing();
		cir.setReturnValue(false);
	}
}
