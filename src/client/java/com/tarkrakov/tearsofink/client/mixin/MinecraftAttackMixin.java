package com.tarkrakov.tearsofink.client.mixin;

import com.tarkrakov.tearsofink.client.SkillKeys;
import com.tarkrakov.tearsofink.client.RangedClient;
import com.tarkrakov.tearsofink.weapon.ranged.RangedItem;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
        if (player != null && com.tarkrakov.tearsofink.weapon.ranged.RangedItem.kind(player.getMainHandItem()) != null) {
            com.tarkrakov.tearsofink.client.RangedClient.fire(false);
            cir.setReturnValue(false);
            return;
        }
		if (player == null || player.isSpectator() || Archetype.of(player.getMainHandItem()) == null) return;
		if (Archetype.of(player.getMainHandItem()) == Archetype.BASTION && !com.tarkrakov.tearsofink.weapon.combat.Combat.bastion(player)) return;
		CombatClient.requestSwing();
		cir.setReturnValue(false);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void tearsofink$releaseBeforeUsingNewLoadout(CallbackInfo ci) {
        Minecraft mc=(Minecraft)(Object)this;
        if (SkillKeys.suppressHeldUse(mc)) {RangedClient.beginUseAttempt();ci.cancel();return;}
        if (RangedClient.holdingKnifeAim(mc)) {ci.cancel();return;}
        RangedClient.beginUseAttempt();
	}

    // Vanilla tries entity and block interaction first. Only its fallback to
    // using the held item becomes aiming; a consumed interaction never aims.
    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult tearsofink$aimAfterInteraction(MultiPlayerGameMode mode, Player actor, InteractionHand hand, Operation<InteractionResult> original) {
        if(hand==InteractionHand.MAIN_HAND&&RangedItem.kind(actor.getMainHandItem())!=null){
            RangedClient.aimAfterInteraction((Minecraft)(Object)this);
            return InteractionResult.CONSUME;
        }
        return original.call(mode,actor,hand);
    }

	@Inject(method = "startUseItem", at = @At("TAIL"))
	private void tearsofink$singleParryPerPress(CallbackInfo ci) {
		if (player == null || !player.isUsingItem()) return;
		if (com.tarkrakov.tearsofink.weapon.combat.PaleLongswordMoves.dual(player)
				|| (com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.isWeapon(player.getMainHandItem())
				&& !com.tarkrakov.tearsofink.weapon.combat.PaleGreatswordMoves.dual(player)))
			SkillKeys.suppressUseUntilRelease((Minecraft) (Object) this);
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void tearsofink$holdToMine(boolean leftClick, CallbackInfo ci) {
        if (player != null && com.tarkrakov.tearsofink.weapon.ranged.RangedItem.kind(player.getMainHandItem()) != null) { ci.cancel(); return; }
		if (!leftClick || player == null || Archetype.of(player.getMainHandItem()) == null) return;
		if (Archetype.of(player.getMainHandItem()) == Archetype.BASTION && !com.tarkrakov.tearsofink.weapon.combat.Combat.bastion(player)) return;
		if (CombatClient.attackHeld() < CombatClient.MINING_HOLD) ci.cancel();
	}
}
