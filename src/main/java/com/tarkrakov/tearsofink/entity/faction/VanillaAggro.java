package com.tarkrakov.tearsofink.entity.faction;

import com.tarkrakov.tearsofink.mixin.MobAccessor;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;

/**
 * Ванильные монстры в Чернильных Землях сами нападают на мобов наших фракций (бледные патрули и др.),
 * а не только отвечают на удар. Криперы и нейтральные (эндермены, зомби-пиглины) — как раньше.
 * Игрок для них важнее: цель «фракция» стоит после цели «игрок».
 */
public final class VanillaAggro {
	/** Приоритет цели (у зомби/скелетов: 1 — ответ на удар, 2 — игрок). */
	public static final int PRIORITY = 3;

	private VanillaAggro() {}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Monster monster) || entity instanceof FactionMember) return;
			if (entity instanceof Creeper || entity instanceof NeutralMob) return;
			if (level.dimension() != ModDimensions.INK_LANDS) return;
			((MobAccessor) monster).tearsofink$targetSelector().addGoal(PRIORITY,
					new NearestAttackableTargetGoal<>(monster, LivingEntity.class, 10, true, false, VanillaAggro::isFactionTarget));
		});
	}

	private static boolean isFactionTarget(LivingEntity e) {
		if (!(e instanceof FactionMember m)) return false;
		Faction f = m.faction();
		return f == Faction.INK || f == Faction.BLOOD || f == Faction.FISH;
	}
}
