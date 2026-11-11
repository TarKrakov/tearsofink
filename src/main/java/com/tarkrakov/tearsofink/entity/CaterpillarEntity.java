package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Гусеница — мирная фауна Чернильных Земель. Урона не наносит, только получает.
 * Вариант выбирается по зоне появления: бледная (Бледнолесье), кровавая (Кроволесье), чернильная (Чернолесье).
 * Модель одна, текстура, размер, скорость, повадки и добыча — свои у каждого варианта.
 */
public class CaterpillarEntity extends PathfinderMob {
	/** Варианты. Цифры (размер, здоровье, скорость) — здесь. */
	public enum Variant {
		PALE("pale", 1.0f, 6, 0.18),
		BLOOD("blood", 1.45f, 12, 0.15),
		INK("ink", 1.0f, 6, 0.26);

		public final String name;
		public final float scale;
		public final double health;
		public final double speed;

		Variant(String name, float scale, double health, double speed) {
			this.name = name;
			this.scale = scale;
			this.health = health;
			this.speed = speed;
		}

		public static Variant byId(int id) {
			return values()[Math.floorMod(id, values().length)];
		}
	}

	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CaterpillarEntity.class, EntityDataSerializers.INT);
	/** Событие «укусила» — сервер шлёт клиентам, те запускают анимацию bite. */
	private static final byte BITE_EVENT = 4;
	/** Длина анимации bite из Blockbench, в тиках (2 с). */
	private static final int BITE_TICKS = 40;

	// Анимации (только на клиенте)
	public final AnimationState idleState = new AnimationState();
	public final AnimationState biteState = new AnimationState();
	private int biteTicksLeft;

	public CaterpillarEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 6)
				.add(Attributes.MOVEMENT_SPEED, 0.18);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(VARIANT, Variant.PALE.ordinal());
	}

	public Variant variant() {
		return Variant.byId(this.entityData.get(VARIANT));
	}

	public void setVariant(Variant variant) {
		this.entityData.set(VARIANT, variant.ordinal());
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(variant.health);
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant.speed);
		this.setHealth((float) variant.health);
		this.refreshDimensions();
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (VARIANT.equals(key)) this.refreshDimensions();
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(variant().scale);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
		// бледные пугливые — держатся подальше от игроков
		this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5f, 1.2, 1.4,
				e -> variant() == Variant.PALE));
		this.goalSelector.addGoal(3, new EatPlantGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 4f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Что гусеница ест: бледная — Слёзницу и бледную траву, чернильная — травы и осоку Чернолесья. */
	public boolean isFood(BlockState state) {
		return switch (variant()) {
			case PALE -> state.is(ModBlocks.TEARBLOOM) || state.is(ModBlocks.PALE_GRASS);
			case INK -> state.is(ModBlocks.INKROOT) || state.is(ModBlocks.TARWEED) || state.is(ModBlocks.INK_GRASS);
			case BLOOD -> false; // кровавые пьют из Кровосборников (появятся вместе с ними)
		};
	}

	// ----- Появление: вариант по зоне -----
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
										@Nullable SpawnGroupData groupData) {
		Holder<Biome> biome = level.getBiome(this.blockPosition());
		Variant v = biome.is(ModBiomes.BLOODWOOD) ? Variant.BLOOD
				: biome.is(ModBiomes.INKWOOD) ? Variant.INK
				: Variant.PALE;
		setVariant(v);
		return super.finalizeSpawn(level, difficulty, spawnType, groupData);
	}

	// ----- Чернильный след -----
	@Override
	public void aiStep() {
		super.aiStep();
		if (variant() != Variant.INK || !this.walkAnimation.isMoving()) return;
		if (this.level().isClientSide) {
			if (this.random.nextInt(3) == 0) {
				this.level().addParticle(ModParticles.INK_DRIP_LAND, getX(), getY() + 0.05, getZ(), 0, 0, 0);
			}
		} else if (this.random.nextInt(EntityTuning.INK_PUDDLE_CHANCE) == 0) {
			BlockPos at = this.blockPosition();
			if (this.level().getBlockState(at).isAir() && ModBlocks.DRYING_INK_PUDDLE.defaultBlockState().canSurvive(this.level(), at)) {
				this.level().setBlock(at, ModBlocks.DRYING_INK_PUDDLE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	// ----- Добыча своя у каждого варианта -----
	@Override
	protected ResourceKey<LootTable> getDefaultLootTable() {
		return ResourceKey.create(Registries.LOOT_TABLE, TearsOfInk.id("entities/caterpillar_" + variant().name));
	}

	// ----- Сохранение -----
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("Variant", variant().name);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		for (Variant v : Variant.values()) {
			if (v.name.equals(tag.getString("Variant"))) {
				this.entityData.set(VARIANT, v.ordinal());
				this.refreshDimensions();
			}
		}
	}

	// ----- Звуки (заглушки — ванильная чешуйница, тоньше) -----
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * (variant() == Variant.BLOOD ? 1.0f : 1.6f);
	}

	@Override
	protected float getSoundVolume() {
		return 0.5f;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	/** Укус по цветку — анимация bite у всех, кто видит. */
	public void bite() {
		this.level().broadcastEntityEvent(this, BITE_EVENT);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == BITE_EVENT) {
			biteTicksLeft = BITE_TICKS;
			biteState.start(this.tickCount);
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			if (biteTicksLeft > 0 && --biteTicksLeft == 0) biteState.stop();
			// стоит и не ест — дышит
			idleState.animateWhen(!this.walkAnimation.isMoving() && biteTicksLeft == 0, this.tickCount);
		}
	}

	/** Сервер: поел. */
	public void ate(ServerLevel level) {
		this.heal(2f);
		level.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT, getSoundSource(), 0.4f, 1.6f);
	}
}
