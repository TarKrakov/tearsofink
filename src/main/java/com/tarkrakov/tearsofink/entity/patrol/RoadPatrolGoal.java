package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/** Патруль (лидер отряда или одиночка): ходит туда-обратно по своему отрезку дороги. */
public class RoadPatrolGoal extends Goal {
	private final PaleSoldier mob;
	private boolean forward = true;
	private int pause;

	public RoadPatrolGoal(PaleSoldier mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (mob.isCampGuard() || mob.getTarget() != null || mob.patrolRoad < 0 || !mob.isLeader()) return false;
		if (pause > 0) {
			pause--;
			return false;
		}
		return mob.level() instanceof ServerLevel;
	}

	@Override
	public boolean canContinueToUse() {
		return mob.getTarget() == null && !mob.getNavigation().isDone();
	}

	@Override
	public void start() {
		ServerLevel level = (ServerLevel) mob.level();
		InkLandsLayout layout = InkLandsLayout.of(level);
		if (layout == null) return;
		double d = forward ? mob.patrolTo : mob.patrolFrom;
		forward = !forward;
		double[] p = layout.roadPoint(mob.patrolRoad, d);
		int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
		if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		mob.getNavigation().moveTo(x + 0.5, y, z + 0.5, 0.8);
	}

	@Override
	public void stop() {
		pause = 40 + mob.getRandom().nextInt(80); // постоял, огляделся
	}
}
