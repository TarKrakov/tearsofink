package com.tarkrakov.tearsofink.entity.spearman;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Бой копейщика: держит дистанцию копья, тычет, отпрыгивает, если игрок влез вплотную. */
public class SpearmanCombatGoal extends Goal {
	private final PaleSpearmanEntity mob;
	private int repath;

	public SpearmanCombatGoal(PaleSpearmanEntity mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null) return;
		mob.getLookControl().setLookAt(target, 30f, 30f);
		if (mob.busy()) {
			mob.getNavigation().stop();
			return;
		}
		double dist = mob.distanceTo(target);

		// вплотную — отпрыгнуть
		if (dist < SpearmanTuning.DODGE_CLOSE && mob.dodgeCooldown <= 0 && mob.onGround()) {
			mob.startDodge(target);
			return;
		}
		// на дистанции копья — тычок
		if (dist <= SpearmanTuning.THRUST_START && mob.attackCooldown <= 0 && mob.hasLineOfSight(target)) {
			mob.getNavigation().stop();
			mob.startThrust();
			return;
		}
		// подойти, но не вплотную
		if (dist > SpearmanTuning.KEEP_DISTANCE) {
			if (--repath <= 0) {
				repath = 8;
				mob.getNavigation().moveTo(target, 1.0);
			}
		} else {
			mob.getNavigation().stop();
		}
	}
}
