package com.tarkrakov.tearsofink.entity.faction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Отношения фракций. Таблицу можно крутить самому.
 *   ALLY    — свои: не нападают, не бьют друг друга (стрелы пролетают мимо), не мстят.
 *   NEUTRAL — сами не нападают, но если ударить — ответят.
 *   HOSTILE — нападают первыми, как только заметят.
 * Существа без фракции (гусеницы, животные) никого не волнуют.
 */
public final class Factions {
	public enum Relation { ALLY, NEUTRAL, HOSTILE }

	private static final Relation A = Relation.ALLY, N = Relation.NEUTRAL, H = Relation.HOSTILE;

	/** Строка — кто смотрит, столбец — на кого. Порядок: INK, BLOOD, FISH, PLAYER, MONSTER, HUNTER, FERAL. */
	private static final Relation[][] TABLE = {
			//            INK BLOOD FISH PLAYER MONSTER HUNTER FERAL
			/* INK     */ {A,  H,    H,   H,     H,      N,     H},
			/* BLOOD   */ {H,  A,    H,   N,     H,      N,     H}, // к игроку пока нейтральны (позже — от репутации/стадии Души)
			/* FISH    */ {H,  H,    A,   H,     H,      H,     H},
			/* PLAYER  */ {H,  N,    H,   N,     H,      N,     H}, // строка игрока ни на что не влияет (игрок решает сам), для полноты
			/* MONSTER */ {H,  H,    H,   H,     A,      H,     H}, // ванильные в Землях нападают сами (VanillaAggro); криперы и нейтральные — нет
			/* HUNTER  */ {N,  N,    N,   N,     N,      A,     N}, // охотники Шпиля и Крови: только отвечают на удар
			/* FERAL   */ {H,  H,    H,   N,     H,      N,     A}, // Чернильный: воюет со всеми, на игроков — только в бешенстве (Охота)
	};

	/** Криперов фракции не трогают первыми (иначе взорвут отряд). Ударит — тогда да. */
	public static final boolean IGNORE_CREEPERS = true;

	private Factions() {}

	@Nullable
	public static Faction of(@Nullable Entity e) {
		if (e instanceof FactionMember m) return m.faction();
		if (e instanceof Player) return Faction.PLAYER;
		if (e instanceof Enemy) return Faction.MONSTER;
		return null;
	}

	public static Relation relation(@Nullable Faction from, @Nullable Faction to) {
		if (from == null || to == null) return Relation.NEUTRAL;
		return TABLE[from.ordinal()][to.ordinal()];
	}

	public static Relation relation(Entity from, Entity to) {
		return relation(of(from), of(to));
	}

	public static boolean allied(Entity a, Entity b) {
		return of(a) != null && relation(a, b) == Relation.ALLY;
	}

	/** Нападает ли {@code self} на {@code target} первым. */
	public static boolean wantsToAttack(LivingEntity self, LivingEntity target) {
		if (IGNORE_CREEPERS && target instanceof Creeper) return false;
		return relation(self, target) == Relation.HOSTILE;
	}
}
