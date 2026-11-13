package com.tarkrakov.tearsofink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tarkrakov.tearsofink.entity.faction.FactionMember;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Урон наших мобов по игроку не зависит от сложности: всегда как на «нормальной».
 * Игра сама умножает урон мобов (лёгкая — меньше, сложная — ×1,5); для наших мы заранее делаем обратное.
 * Ванильные мобы — как обычно.
 */
@Mixin(Player.class)
public abstract class PlayerDifficultyMixin {
	@WrapMethod(method = "hurt")
	private boolean tearsofink$sameOnAnyDifficulty(DamageSource source, float amount, Operation<Boolean> original) {
		Player self = (Player) (Object) this;
		if (source.scalesWithDifficulty() && source.getEntity() instanceof FactionMember) {
			Difficulty d = self.level().getDifficulty();
			if (d == Difficulty.HARD) amount = amount / 1.5f;
			else if (d == Difficulty.EASY && amount > 2f) amount = 2f * (amount - 1f); // лёгкая: x/2+1 → обратно
		}
		return original.call(source, amount);
	}
}
