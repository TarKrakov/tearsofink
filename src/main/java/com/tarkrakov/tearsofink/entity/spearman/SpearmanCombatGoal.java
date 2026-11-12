package com.tarkrakov.tearsofink.entity.spearman;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Бой копейщика: держит дистанцию копья, тычет, отпрыгивает, если игрок влез вплотную. */
public class SpearmanCombatGoal extends Goal {
	private final PaleSpearmanEntity mob;
	private int repath;
	// набеги
	private int hitsLeft = -1;
	private int retreatTicks = -1, restTicks = -1;
	private net.minecraft.world.phys.Vec3 rally;

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
		hitsLeft = -1;
		retreatTicks = restTicks = -1;
	}

	private boolean shieldbearerAlive() {
		for (var mate : mob.squadmates(40)) {
			if (mate instanceof com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity) return true;
		}
		return false;
	}

	private int newRaid() {
		return SpearmanTuning.RAID_HITS_MIN + mob.getRandom().nextInt(SpearmanTuning.RAID_HITS_MAX - SpearmanTuning.RAID_HITS_MIN + 1);
	}

	/** Куда отходить: за спины арбалетчиков (чуть в сторону), иначе просто назад от игрока. */
	private net.minecraft.world.phys.Vec3 rallyPoint(LivingEntity target) {
		double sx = 0, sz = 0, sy = 0;
		int n = 0;
		for (var mate : mob.squadmates(30)) {
			if (mate instanceof com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity) {
				sx += mate.getX(); sy += mate.getY(); sz += mate.getZ(); n++;
			}
		}
		net.minecraft.world.phys.Vec3 away;
		if (n > 0) {
			net.minecraft.world.phys.Vec3 c = new net.minecraft.world.phys.Vec3(sx / n, sy / n, sz / n);
			away = c.subtract(target.position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 1.0E-4) away = new net.minecraft.world.phys.Vec3(1, 0, 0);
			away = away.normalize();
			net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(-away.z, 0, away.x).scale(mob.getRandom().nextBoolean() ? 1.5 : -1.5);
			return c.add(away.scale(1.0)).add(side);
		}
		away = mob.position().subtract(target.position()).multiply(1, 0, 1);
		if (away.lengthSqr() < 1.0E-4) away = new net.minecraft.world.phys.Vec3(1, 0, 0);
		return target.position().add(away.normalize().scale(SpearmanTuning.RAID_FALLBACK));
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null) return;
		if (!mob.reverseDodge) mob.getLookControl().setLookAt(target, 30f, 30f);
		if (mob.busy()) {
			mob.getNavigation().stop();
			return;
		}
		double dist = mob.distanceTo(target);

		// ----- набеги, пока жив щитовик -----
		if (!shieldbearerAlive()) {
			hitsLeft = -1;
			retreatTicks = restTicks = -1;
		} else {
			if (hitsLeft < 0) hitsLeft = newRaid();
			if (hitsLeft == 0 && retreatTicks < 0 && restTicks < 0) { // удары кончились — назад
				retreatTicks = 0;
				rally = rallyPoint(target);
			}
			if (retreatTicks >= 0) {
				retreatTicks++;
				if (dist < SpearmanTuning.DODGE_CLOSE && mob.dodgeCooldown <= 0 && mob.onGround()) {
					mob.startDodge(target);
					return;
				}
				if (retreatTicks % 10 == 1) rally = rallyPoint(target);
				if (mob.position().distanceToSqr(rally) < 2.5 * 2.5 || retreatTicks > SpearmanTuning.RAID_RETREAT_MAX) {
					retreatTicks = -1;
					restTicks = SpearmanTuning.RAID_REST;
					mob.getNavigation().stop();
				} else {
					if (--repath <= 0) {
						repath = 6;
						mob.getNavigation().moveTo(rally.x, rally.y, rally.z, 1.25);
					}
					return;
				}
			}
			if (restTicks >= 0) { // стоит у стрелков: защищается, но не гонится
				if (--restTicks < 0) {
					hitsLeft = newRaid();
				} else {
					if (dist < SpearmanTuning.DODGE_CLOSE && mob.dodgeCooldown <= 0 && mob.onGround()) {
						mob.startDodge(target);
					} else if (dist <= SpearmanTuning.THRUST_START && mob.attackCooldown <= 0 && mob.hasLineOfSight(target)) {
						mob.startThrust();
					} else {
						mob.getNavigation().stop();
					}
					return;
				}
			}
		}

		// вплотную — отпрыгнуть
		if (dist < SpearmanTuning.DODGE_CLOSE && mob.dodgeCooldown <= 0 && mob.onGround()) {
			mob.startDodge(target);
			return;
		}
		// игрок отошёл — «спиной вперёд» прыжком к нему
		if (dist > SpearmanTuning.GAP_MIN && dist < SpearmanTuning.GAP_MAX && mob.gapCooldown <= 0
				&& mob.onGround() && mob.hasLineOfSight(target)) {
			mob.startGapClose(target);
			return;
		}
		// на дистанции копья — тычок
		if (dist <= SpearmanTuning.THRUST_START && mob.attackCooldown <= 0 && mob.hasLineOfSight(target)) {
			mob.getNavigation().stop();
			mob.startThrust();
			if (hitsLeft > 0) hitsLeft--;
			return;
		}
		// в отряде со стрелками — встать между игроком и ближайшим арбалетчиком (прикрыть их)
		com.tarkrakov.tearsofink.entity.patrol.PaleSoldier archer = null;
		double best = 20 * 20;
		for (var mate : mob.squadmates(20)) {
			if (mate instanceof com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity && mate.distanceToSqr(target) < best) {
				best = mate.distanceToSqr(target);
				archer = mate;
			}
		}
		if (archer != null) {
			net.minecraft.world.phys.Vec3 dir = archer.position().subtract(target.position()).multiply(1, 0, 1);
			if (dir.lengthSqr() > 1.0E-4) {
				net.minecraft.world.phys.Vec3 guard = target.position().add(dir.normalize().scale(SpearmanTuning.KEEP_DISTANCE));
				if (--repath <= 0) {
					repath = 8;
					mob.getNavigation().moveTo(guard.x, guard.y, guard.z, 1.15);
				}
				return;
			}
		}
		// подойти, но не вплотную
		if (dist > SpearmanTuning.KEEP_DISTANCE) {
			if (--repath <= 0) {
				repath = 8;
				mob.getNavigation().moveTo(target, 1.15);
			}
		} else {
			mob.getNavigation().stop();
		}
	}
}
