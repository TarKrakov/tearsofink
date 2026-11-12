package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import com.tarkrakov.tearsofink.block.lantern.Lanterns;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Патрули Шпиля гасят фонари: вожак отряда, когда не дерётся, замечает горящий фонарь рядом,
 * подходит и гасит (отряд идёт за ним сам). Любые Чернильные и Каменные фонари — и мировые, и самодельные.
 * Вечные фонари не трогают.
 */
public class DouseLanternGoal extends Goal {
	private final PaleSoldier mob;
	@Nullable private BlockPos lantern;
	private int scan, ticks, repath;

	public DouseLanternGoal(PaleSoldier mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!mob.isLeader() || mob.getTarget() != null) return false;
		if (++scan < SquadTuning.DOUSE_SCAN) return false;
		scan = mob.getRandom().nextInt(10);
		lantern = findLit();
		return lantern != null;
	}

	@Override
	public boolean canContinueToUse() {
		return lantern != null && mob.getTarget() == null && ticks < 400 && isLit(lantern);
	}

	@Override
	public void start() {
		ticks = 0;
		repath = 0;
	}

	@Override
	public void stop() {
		lantern = null;
		mob.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (lantern == null) return;
		ticks++;
		mob.getLookControl().setLookAt(lantern.getX() + 0.5, lantern.getY() + 0.5, lantern.getZ() + 0.5);
		double d2 = mob.distanceToSqr(lantern.getX() + 0.5, lantern.getY() + 0.5, lantern.getZ() + 0.5);
		if (d2 <= 3.2 * 3.2) {
			douse(lantern);
			lantern = null;
			return;
		}
		if (--repath <= 0) {
			repath = 10;
			mob.getNavigation().moveTo(lantern.getX() + 0.5, lantern.getY(), lantern.getZ() + 0.5, 0.9);
		}
	}

	@Nullable
	private BlockPos findLit() {
		BlockPos[] best = {null};
		double[] bestD = {Double.MAX_VALUE};
		Lanterns.forEachLit(mob.level(), mob.getX(), mob.getZ(), SquadTuning.DOUSE_RADIUS, (pos, state, l) -> {
			if (!douseable(state) || Math.abs(pos.getY() - mob.getY()) > 6) return;
			double d = pos.distToCenterSqr(mob.position());
			if (d <= SquadTuning.DOUSE_RADIUS * SquadTuning.DOUSE_RADIUS && d < bestD[0]) {
				bestD[0] = d;
				best[0] = pos.immutable();
			}
		});
		return best[0];
	}

	private static boolean douseable(BlockState state) {
		return !state.is(ModBlocks.ETERNAL_LANTERN) && state.hasProperty(FogClearing.LIT) && state.getValue(FogClearing.LIT);
	}

	private boolean isLit(BlockPos pos) {
		return douseable(mob.level().getBlockState(pos));
	}

	private void douse(BlockPos pos) {
		BlockState state = mob.level().getBlockState(pos);
		if (!douseable(state)) return;
		mob.level().setBlock(pos, state.setValue(FogClearing.LIT, false), Block.UPDATE_ALL);
		mob.swing(InteractionHand.MAIN_HAND);
		mob.level().playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.9f);
		if (mob.level() instanceof ServerLevel server) {
			server.sendParticles(ModParticles.INK_SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 10, 0.2, 0.15, 0.2, 0.0);
		}
	}
}
