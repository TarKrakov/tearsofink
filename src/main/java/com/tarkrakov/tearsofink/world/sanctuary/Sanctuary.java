package com.tarkrakov.tearsofink.world.sanctuary;

import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Храм — мирная зона: вся площадь храма с запасом. Внутри никто не появляется и не заходит
 * (кроме игроков, приручённых питомцев и Торговки), мобы не трогают тех, кто внутри,
 * стрелы и болты снаружи гаснут на границе.
 */
public final class Sanctuary {
	/** Полуширина зоны от центра храма (храм до 33 блоков в длину, плюс запас). */
	public static final int RADIUS = 20;
	/** Зона по высоте: от земли у центра храма вниз и вверх. */
	public static final int DOWN = 28, UP = 16;
	/** Как часто выталкивать мобов (тики). */
	private static final int PUSH_EVERY = 5;
	/** Насколько далеко вокруг мобы забывают цель, стоящую в храме. */
	private static final int CALM_AROUND = 24;

	private Sanctuary() {}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (!ModDimensions.isLands(level)) return;
			InkLandsLayout layout = InkLandsLayout.of(level);
			if (layout == null) return;
			boolean push = level.getGameTime() % PUSH_EVERY == 0;
			for (int[] p : Ruins.templePoints(layout)) tickTemple(level, p[0], p[1], push);
		});
	}

	/** Внутри ли мирной зоны какого-нибудь храма (только по горизонтали — для выбора мест). */
	public static boolean inside(ServerLevel level, double x, double z) {
		return near(level, x, z, 0);
	}

	/** Ближе ли {@code extra} блоков к мирной зоне. */
	public static boolean near(ServerLevel level, double x, double z, int extra) {
		if (!ModDimensions.isLands(level)) return false;
		InkLandsLayout layout = InkLandsLayout.of(level);
		return layout != null && near(layout, x, z, extra);
	}

	public static boolean near(InkLandsLayout layout, double x, double z, int extra) {
		int r = RADIUS + extra;
		for (int[] p : Ruins.templePoints(layout)) {
			if (Math.abs(x - (p[0] + 0.5)) <= r && Math.abs(z - (p[1] + 0.5)) <= r) return true;
		}
		return false;
	}

	/** Сущность в мирной зоне (с учётом высоты). */
	public static boolean contains(Entity e) {
		if (!(e.level() instanceof ServerLevel level) || !ModDimensions.isLands(level)) return false;
		InkLandsLayout layout = InkLandsLayout.of(level);
		if (layout == null) return false;
		for (int[] p : Ruins.templePoints(layout)) {
			if (Math.abs(e.getX() - (p[0] + 0.5)) <= RADIUS && Math.abs(e.getZ() - (p[1] + 0.5)) <= RADIUS) {
				var chunk = level.getChunkSource().getChunkNow(p[0] >> 4, p[1] >> 4);
				if (chunk == null) return true;
				int ground = height(chunk, p[0], p[1]);
				return e.getY() >= ground - DOWN && e.getY() <= ground + UP;
			}
		}
		return false;
	}

	/** Кому можно внутрь. */
	public static boolean allowed(Entity e) {
		if (e instanceof Player || e instanceof SoulTraderEntity) return true;
		return e instanceof OwnableEntity own && own.getOwnerUUID() != null;
	}

	/** Можно ли этому существу нападать на эту цель (цель в храме — нельзя). */
	public static boolean mayAttack(LivingEntity attacker, LivingEntity target) {
		if (!(attacker instanceof Mob) || allowed(attacker) || !allowed(target)) return true;
		return !contains(target);
	}

	private static void tickTemple(ServerLevel level, int cx, int cz, boolean push) {
		// The holder can exist before FULL is ready. A height lookup through the level
		// would then run a synchronous chunk request inside the server tick.
		var chunk = level.getChunkSource().getChunkNow(cx >> 4, cz >> 4);
		if (chunk == null) return;
		int ground = height(chunk, cx, cz);
		double x0 = cx + 0.5 - RADIUS, x1 = cx + 0.5 + RADIUS, z0 = cz + 0.5 - RADIUS, z1 = cz + 0.5 + RADIUS;
		AABB zone = new AABB(x0, ground - DOWN, z0, x1, ground + UP, z1);

		// стрелы, болты и прочее, пущенное не игроком, гаснут в зоне
		for (Projectile pr : level.getEntitiesOfClass(Projectile.class, zone,
				pr -> !(pr.getOwner() instanceof Player))) {
			pr.discard();
		}
		if (!push) return;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, zone.inflate(CALM_AROUND), m -> m.isAlive() && !allowed(m))) {
			LivingEntity target = mob.getTarget();
			if (target != null && zone.contains(target.position())) mob.setTarget(null);
			if (!zone.contains(mob.position())) continue;
			pushOut(level, mob, cx + 0.5, cz + 0.5);
		}
	}

	/** Выталкивает к ближайшему краю: у края — толчком, глубоко внутри — переносом за границу. */
	private static void pushOut(ServerLevel level, Mob mob, double cx, double cz) {
		double dx = mob.getX() - cx, dz = mob.getZ() - cz;
		double depth;
		Vec3 dir;
		if (Math.abs(dx) >= Math.abs(dz)) {
			depth = RADIUS - Math.abs(dx);
			dir = new Vec3(Math.signum(dx == 0 ? 1 : dx), 0, 0);
		} else {
			depth = RADIUS - Math.abs(dz);
			dir = new Vec3(0, 0, Math.signum(dz));
		}
		mob.getNavigation().stop();
		if (depth <= 2.5) {
			mob.setDeltaMovement(dir.x * 0.5, Math.max(mob.getDeltaMovement().y, 0.1), dir.z * 0.5);
			mob.hurtMarked = true;
			return;
		}
		double tx = dir.x != 0 ? cx + dir.x * (RADIUS + 2) : mob.getX();
		double tz = dir.z != 0 ? cz + dir.z * (RADIUS + 2) : mob.getZ();
		BlockPos at = BlockPos.containing(tx, 0, tz);
		if (!level.hasChunkAt(at)) {
			mob.discard();
			return;
		}
		var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
		if (chunk != null) mob.teleportTo(tx, height(chunk, at.getX(), at.getZ()), tz);
	}

	private static int height(LevelChunk chunk, int x, int z) {
		return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
	}
}
