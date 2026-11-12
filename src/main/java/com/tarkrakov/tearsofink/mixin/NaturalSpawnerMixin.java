package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.herald.BloodHerald;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ванильные монстры не появляются сами по себе возле Багряных Вестников:
 * огромная крона даёт тень, и под ней иначе кишели бы зомби. Наших мобов это не касается.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void tearsofink$noMonstersUnderHerald(ServerLevel level, Mob mob, double distance,
														 CallbackInfoReturnable<Boolean> cir) {
		if (!(mob instanceof Enemy)) return;
		if (!"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace())) return;
		if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof InkLandsAccess access)) return;
		InkLandsLayout layout = access.tearsofink$layout(level.getChunkSource().randomState().sampler());
		if (layout != null && layout.nearHerald(mob.getBlockX(), mob.getBlockZ(), BloodHerald.NO_MONSTER_RADIUS)) {
			cir.setReturnValue(false);
		}
	}
}
