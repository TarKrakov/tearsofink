package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Чернильная клякса (абилка длинного ножа): шарик чернил, малый урон, ненадолго замедляет. */
public class InkBlotEntity extends ThrowableItemProjectile {
	public static final float DAMAGE = 3f;
	public static final int SLOW_TICKS = 30;

	public InkBlotEntity(EntityType<? extends InkBlotEntity> type, Level level) {
		super(type, level);
	}

	public InkBlotEntity(Level level, LivingEntity owner) {
		super(ModEntities.INK_BLOT, owner, level);
	}

	/** Почти не падает — летит дальше (обычный снежок: 0.03). */
	@Override
	protected double getDefaultGravity() {
		return 0.012;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.INK_BLOT;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide && tickCount % 2 == 0) {
			level().addParticle(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof LivingEntity target && target != getOwner()) {
			if (getOwner() instanceof Player p) target.hurt(damageSources().thrown(this, p), DAMAGE);
			else target.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0));
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 14, 0.2, 0.2, 0.2, 0.05);
			server.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.8f, 0.7f);
			discard();
		}
	}
}
