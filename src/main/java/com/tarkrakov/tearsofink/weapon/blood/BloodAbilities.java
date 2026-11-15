package com.tarkrakov.tearsofink.weapon.blood;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.combat.Archetype;
import com.tarkrakov.tearsofink.weapon.combat.Combat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Приёмы оружия Крови — платят здоровьем (цена — BloodTuning). */
public final class BloodAbilities {
	public static final BloodArcAbility BLOOD_ARC = new BloodArcAbility();
	public static final TalwarCrossAbility TALWAR_CROSS = new TalwarCrossAbility();
	public static final ParryAbility PARRY = new ParryAbility();
	public static final DoubleParryAbility DOUBLE_PARRY = new DoubleParryAbility();

	private BloodAbilities() {}

	static Vec3 flatLook(ServerPlayer p) {
		Vec3 l = p.getLookAngle();
		Vec3 f = new Vec3(l.x, 0, l.z);
		return f.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** Тальвар (R): росчерк — короткая широкая кровавая дуга с кровотечением. */
	public static class BloodArcAbility extends Skill {
		BloodArcAbility() {
			super(TearsOfInk.id("blood_arc"), 0, 0, BloodTuning.ARC_COOLDOWN);
		}

		@Override
		public float healthCost() {
			return BloodTuning.ARC_HEALTH;
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			float power = WeaponAbilities.power(player);
			level.addFreshEntity(new BloodArcEntity(level, player, flatLook(player), false, BloodTuning.ARC_DAMAGE * power, BloodTuning.ARC_BLEED));
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.2f);
			level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8f, 0.6f);
			return true;
		}
	}

	/** Два Тальвара (ПКМ): крест-накрест — кровавый крест вперёд и отскок назад. */
	public static class TalwarCrossAbility extends Skill {
		TalwarCrossAbility() {
			super(TearsOfInk.id("talwar_cross"), 0, 0, BloodTuning.CROSS_COOLDOWN);
		}

		@Override
		public float healthCost() {
			return BloodTuning.CROSS_HEALTH;
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			ServerLevel level = player.serverLevel();
			Vec3 dir = flatLook(player);
			level.addFreshEntity(new BloodArcEntity(level, player, dir, true, BloodTuning.CROSS_DAMAGE, BloodTuning.CROSS_BLEED));
			Combat.dualAnim(player, Archetype.TALWAR, "cross_burst", "cross_burst", 14);
			// отскок назад
			player.setDeltaMovement(-dir.x * BloodTuning.CROSS_BACKSTEP, 0.3, -dir.z * BloodTuning.CROSS_BACKSTEP);
			player.hurtMarked = true;
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.7f);
			return true;
		}
	}

	/** Дуссак (R): стойка парирования — удар вблизи обнуляется, клинок отвечает росчерком с кровотечением. */
	public static class ParryAbility extends Skill {
		ParryAbility() {
			super(TearsOfInk.id("dussak_parry"), 0, 0, BloodTuning.PARRY_COOLDOWN);
		}

		@Override
		public float healthCost() {
			return BloodTuning.PARRY_HEALTH;
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (Parry.active(player)) return false;
			Parry.start(player, false); // движение стойки — ability_anim из dussak.json
			return true;
		}
	}

	/** Два Дуссака (ПКМ): усиленная стойка — дольше, отвечают оба клинка, кровотечение сильнее. */
	public static class DoubleParryAbility extends Skill {
		DoubleParryAbility() {
			super(TearsOfInk.id("dussak_double_parry"), 0, 0, BloodTuning.DOUBLE_PARRY_COOLDOWN);
		}

		@Override
		public float healthCost() {
			return BloodTuning.DOUBLE_PARRY_HEALTH;
		}

		@Override
		public boolean activate(ServerPlayer player, SkillInput input) {
			if (Parry.active(player)) return false;
			Parry.start(player, true);
			return true;
		}
	}
}
