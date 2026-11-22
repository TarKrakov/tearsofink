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
	/** В храмах (мирная зона) не появляется никто. */
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void tearsofink$noSpawnInTemples(ServerLevel level, Mob mob, double distance,
													CallbackInfoReturnable<Boolean> cir) {
        // The natural-spawn path includes vanilla creatures and caterpillars.
        // Explicit/quest entities are not created through this validation.
        if (com.tarkrakov.tearsofink.world.battlefield.BattlefieldFaunaBoundary.inside(
                level, mob.getX(), mob.getZ())
                && !(mob instanceof com.tarkrakov.tearsofink.entity.PaleStriderEntity)) {
            cir.setReturnValue(false);
            return;
        }
		if (com.tarkrakov.tearsofink.world.sanctuary.Sanctuary.inside(level, mob.getX(), mob.getZ())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void tearsofink$noMonstersUnderHerald(ServerLevel level, Mob mob, double distance,
														 CallbackInfoReturnable<Boolean> cir) {
		if (!(mob instanceof Enemy)) return;
		if(com.tarkrakov.tearsofink.entity.patrol.BarricadeGates.suppressesSpawns(level,mob.blockPosition())) {
			cir.setReturnValue(false);return;
		}
		if (!"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace())) return;
		// D-090: natural vanilla enemies are absent throughout peaceful Bloodwood.
		// This does not cancel caterpillars, custom neutral fauna, or explicit Harvest event spawns.
		if(com.tarkrakov.tearsofink.world.ModDimensions.isLands(level)
				&& (level.getBiome(mob.blockPosition()).is(com.tarkrakov.tearsofink.world.ModBiomes.BLOODWOOD)
				||com.tarkrakov.tearsofink.world.herald.BloodSettlements.contains(level,mob.blockPosition()))){
			cir.setReturnValue(false);return;
		}
		// D-101: reject most natural vanilla hostile-mob candidates; leave scripted
		// encounters and mod fauna alone. Inkwood retains more enemies for metal farming.
		if (com.tarkrakov.tearsofink.world.ModDimensions.isLands(level)) {
			int acceptance = level.getBiome(mob.blockPosition()).is(com.tarkrakov.tearsofink.world.ModBiomes.INKWOOD) ? 35 : 10;
			if (level.getRandom().nextInt(100) >= acceptance) {
				cir.setReturnValue(false);return;
			}
		}
		if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof InkLandsAccess access)) return;
		InkLandsLayout layout = access.tearsofink$layout(level.getChunkSource().randomState().sampler());
		if (layout != null && layout.nearHerald(mob.getBlockX(), mob.getBlockZ(), BloodHerald.NO_MONSTER_RADIUS)) {
			cir.setReturnValue(false);
		}
	}
}
