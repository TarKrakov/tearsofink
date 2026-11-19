package com.tarkrakov.tearsofink.entity.patrol;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** В патруле — идти колонной за лидером отряда (позади на своё место в строю). */
public class FollowLeaderGoal extends Goal {
	private final PaleSoldier mob;
	private final double slot;
	private int repath;

	/** @param slot сколько блоков позади лидера держаться */
	public FollowLeaderGoal(PaleSoldier mob, double slot) {
		this.mob = mob;
		this.slot = slot;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (mob.isCampGuard() || mob.getTarget() != null || mob.isLeader()) return false;
		PaleSoldier leader = mob.leaderEntity();
		return leader != null && leader.getTarget() == null && mob.distanceToSqr(spot(leader)) > 4;
	}

	@Override
	public boolean canContinueToUse() {
		PaleSoldier leader = mob.leaderEntity();
		return mob.getTarget() == null && leader != null && mob.distanceToSqr(spot(leader)) > 2;
	}

	private Vec3 spot(PaleSoldier leader) {
		Vec3 back = leader.getLookAngle().multiply(1, 0, 1);
		if (back.lengthSqr() < 1.0E-4) back = new Vec3(0, 0, 1);
		return leader.position().subtract(back.normalize().scale(slot));
	}

	@Override
	public void tick() {
		PaleSoldier leader = mob.leaderEntity();
		if (leader == null) return;
		if (--repath <= 0) {
			repath = 10;
			Vec3 s = spot(leader);
			mob.getNavigation().moveTo(s.x, s.y, s.z, mob.distanceToSqr(s) > 64 ? 1.1 : 0.85);
		}
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
	}
}
