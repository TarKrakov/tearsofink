package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.block.blood.BloodTankBlockEntity;
import com.tarkrakov.tearsofink.block.blood.BloodTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Гусеница ест растения: находит своё растение → подползает → кусает (растение падает предметом)
 * → подбирает и съедает. Раз в несколько минут, чтобы поляны не исчезали.
 * Если в мире выключено разрушение мобами (mobGriefing) — не ломает растения.
 * Кровавые: чаще пьют из Накопителя Кровосборника (если там больше минимума), иначе — объедают
 * Плотеягоду, не ломая куст.
 */
public class EatPlantGoal extends Goal {
	private final CaterpillarEntity mob;
	private int cooldown;
	private BlockPos plant;
	private BlockPos tank;
	private ItemEntity food;
	private int ticks;

	public EatPlantGoal(CaterpillarEntity mob) {
		this.mob = mob;
		this.cooldown = mob.getRandom().nextInt(EntityTuning.EAT_COOLDOWN_MIN);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (--cooldown > 0) return false;
		if (!(mob.level() instanceof ServerLevel level)) return false;
		if (!LoadedFaunaTerrain.search(level)) {
			cooldown = 20;
			return false;
		}
		tank = null;
		plant = null;
		// кровавые — сначала попить (иногда всё же идут за ягодой)
		if (mob.variant() == CaterpillarEntity.Variant.BLOOD
				&& mob.getRandom().nextInt(100) >= BloodTuning.BERRY_INSTEAD_PERCENT) {
			tank = BloodTankBlockEntity.findSippable(level, mob.blockPosition(), BloodTuning.CATERPILLAR_SEARCH);
			if (tank != null) return true;
		}
		plant = findPlant(level);
		if (plant != null && !mob.nibbles(level.getBlockState(plant))
				&& !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			plant = null; // ломать растения нельзя
		}
		if (plant == null) {
			cooldown = 400;
			return false;
		}
		return true;
	}

	private BlockPos findPlant(ServerLevel level) {
		BlockPos origin = mob.blockPosition();
		int r = EntityTuning.EAT_SEARCH_RADIUS;
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -2, -r), origin.offset(r, 2, r))) {
			LevelChunk chunk = LoadedFaunaTerrain.chunk(level, p);
			if (chunk == null || !mob.isFood(chunk.getBlockState(p))) continue;
			double d = p.distSqr(origin);
			if (d < bestDist) {
				bestDist = d;
				best = p.immutable();
			}
		}
		return best;
	}

	@Override
	public void start() {
		ticks = 0;
		food = null;
		BlockPos target = tank != null ? tank : plant;
		mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
	}

	@Override
	public boolean canContinueToUse() {
		return ticks < 400 && (tank != null || plant != null || (food != null && food.isAlive()));
	}

	@Override
	public void tick() {
		ticks++;
		ServerLevel level = (ServerLevel) mob.level();
		if (tank != null) {
			if (LoadedFaunaTerrain.chunk(level, tank) == null
					|| !(level.getBlockEntity(tank) instanceof BloodTankBlockEntity t) || !t.canSip()) {
				tank = null; // выпили до нас — в следующий раз
				return;
			}
			mob.getLookControl().setLookAt(tank.getX() + 0.5, tank.getY() + 0.5, tank.getZ() + 0.5);
			if (mob.position().distanceToSqr(tank.getX() + 0.5, tank.getY(), tank.getZ() + 0.5) < 3.0) {
				t.sip();
				mob.bite();
				mob.ate(level);
				tank = null;
			} else if (ticks % 20 == 0) {
				mob.getNavigation().moveTo(tank.getX() + 0.5, tank.getY(), tank.getZ() + 0.5, 1.0);
			}
			return;
		}
		if (plant != null) {
			if (LoadedFaunaTerrain.chunk(level, plant) == null || !mob.isFood(level.getBlockState(plant))
					|| (!mob.nibbles(level.getBlockState(plant))
					&& !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))) {
				plant = null;
				return;
			}
			mob.getLookControl().setLookAt(plant.getX() + 0.5, plant.getY() + 0.3, plant.getZ() + 0.5);
			if (mob.position().distanceToSqr(plant.getX() + 0.5, plant.getY(), plant.getZ() + 0.5) < 2.2) {
				mob.bite();
				if (mob.nibbles(level.getBlockState(plant))) {
					// объела ягоды, куст остался
					plant = null;
					mob.ate(level);
					return;
				}
				BlockPos at = plant;
				plant = null;
				level.destroyBlock(at, true, mob);
				List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(1.5),
						e -> e.getItem().getItem() instanceof BlockItem bi && mob.isFood(bi.getBlock().defaultBlockState()));
				if (dropped.isEmpty()) {
					mob.ate(level); // трава часто ничего не роняет — просто съела
				} else {
					food = dropped.get(0);
					mob.getNavigation().moveTo(food, 1.0);
				}
			} else if (ticks % 20 == 0) {
				mob.getNavigation().moveTo(plant.getX() + 0.5, plant.getY(), plant.getZ() + 0.5, 1.0);
			}
			return;
		}
		if (food != null && food.isAlive()) {
			if (mob.distanceToSqr(food) < 1.2) {
				food.getItem().shrink(1);
				if (food.getItem().isEmpty()) food.discard();
				mob.bite();
				mob.ate(level);
				food = null;
			} else if (ticks % 20 == 0) {
				mob.getNavigation().moveTo(food, 1.0);
			}
		}
	}

	@Override
	public void stop() {
		plant = null;
		tank = null;
		food = null;
		mob.getNavigation().stop();
		cooldown = EntityTuning.EAT_COOLDOWN_MIN + mob.getRandom().nextInt(EntityTuning.EAT_COOLDOWN_SPREAD);
	}
}
