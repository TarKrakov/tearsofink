package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.weapon.InkGreatswordItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Удар эфесом (ПКМ): одноручный клинок архетипа в правой руке, левая пустая. Короткий тупой тычок навершием вперёд:
 * почти без урона, но сбивает замах врага (рывок щитоносца, тычок копейщика, прицел арбалетчика, приёмы охотников)
 * и ненадолго его оглушает. Без замаха — лёгкое отбрасывание. Бесплатно, с перезарядкой.
 * ПКМ по блоку (дверь, сундук, просто камень) — как обычно, эфес не бьёт.
 * Только в Чернильных землях. Движение — "hilt" в weapon_anims/&lt;архетип&gt;.json.
 */
public final class HiltStrike {
	// ===== Числа — крутить здесь =====
	/** Урон (полсердца = 1). */
	public static final float DAMAGE = 1.5f;
	/** Дальность — как у обычного удара. */
	public static final double REACH = 3.0;
	/** Перезарядка, тики (20 = 1 с). */
	public static final int COOLDOWN = 50;
	/** Когда тычок доходит (тики после нажатия) и сколько длится движение. */
	public static final int HIT_TICK = 3, ANIM_TICKS = 9;
	/** Насколько сбитый враг теряется (тики): не бьёт, не ходит. */
	public static final int STAGGER = 20;
	/** Отбрасывание: просто так / если сбил замах. */
	public static final float KNOCKBACK = 0.35f, KNOCKBACK_INTERRUPT = 0.6f;

	private record Pending(ServerPlayer player, long at) {}

	private static final List<Pending> PENDING = new ArrayList<>();
	private static final java.util.Map<java.util.UUID,Integer> READY_AT = new java.util.HashMap<>();
	public static int remaining(ServerPlayer p) { return Math.max(0,READY_AT.getOrDefault(p.getUUID(),0)-p.server.getTickCount()); }
	public static void cancelForAction(ServerPlayer p) { PENDING.removeIf(h -> h.player == p); }

	private HiltStrike() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(HiltStrike::tick);
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->READY_AT.remove(h.getPlayer().getUUID()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{READY_AT.clear(); PENDING.clear();});
	}

	/** Подходит ли сейчас удар эфесом (одинаково у игрока и на сервере). */
	public static boolean applies(Player p, ItemStack stack, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || p.getMainHandItem() != stack) return false;
		if (!ModDimensions.hasLandsRules(p.level()) || p.isSpectator()) return false;
		if (Archetype.of(stack) == null || InkGreatswordItem.twoHanded(stack)) return false;
		if (stack.getItem() instanceof com.tarkrakov.tearsofink.weapon.SoulShieldItem) return false;
		if (!p.getOffhandItem().isEmpty()) return false;
		return !aimsAtBlock(p);
	}

	/** Смотрит на блок ближе, чем на существо, — ПКМ по блоку работает как обычно. */
	private static boolean aimsAtBlock(Player p) {
		HitResult block = p.pick(p.blockInteractionRange(), 0, false);
		if (block.getType() != HitResult.Type.BLOCK) return false;
		EntityHitResult e = target(p, REACH);
		return e == null || e.getLocation().distanceToSqr(p.getEyePosition()) > block.getLocation().distanceToSqr(p.getEyePosition());
	}

	@Nullable
	private static EntityHitResult target(Player p, double reach) {
		Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
		Vec3 end = eye.add(look.scale(reach));
		AABB box = p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
		return ProjectileUtil.getEntityHitResult(p, eye, end, box,
				e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != p && e.isPickable(), reach * reach);
	}

	/** Нажатие ПКМ на сервере. false — не вышло (перезарядка, клинок посреди удара). */
	public static boolean use(ServerPlayer p, ItemStack stack) {
		if (remaining(p) > 0) return false;
		CombatTransitions.begin(p,false);
		Archetype a = Archetype.of(stack);
		READY_AT.put(p.getUUID(),p.server.getTickCount()+COOLDOWN);
		Combat.play(p, a, "hilt", null, ANIM_TICKS);
		long now = p.serverLevel().getGameTime();
		PENDING.add(new Pending(p, now + HIT_TICK));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.7f, 0.7f);
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) return;
		List<Pending> due = new ArrayList<>();
		PENDING.removeIf(h -> {
			if (h.player.isRemoved() || !h.player.isAlive()) return true;
			if (h.player.serverLevel().getGameTime() < h.at) return false;
			due.add(h);
			return true;
		});
		for (Pending h : due) hit(h.player);
	}

	private static void hit(ServerPlayer p) {
		ItemStack stack = p.getMainHandItem();
		if (Archetype.of(stack) == null || !ModDimensions.hasLandsRules(p.level())) return; // сменил клинок / ушёл — тычок пропал
		ServerLevel level = p.serverLevel();
		EntityHitResult hit = target(p, REACH);
		if (hit == null || !(hit.getEntity() instanceof LivingEntity t)) return;
		Entity e = hit.getEntity();
		boolean interrupted = false;
		if (t instanceof Interruptible i) interrupted = i.interrupt(p, STAGGER);
		else if (t instanceof ServerPlayer other) interrupted = Combat.interrupt(other, STAGGER);
		t.hurt(p.damageSources().playerAttack(p), DAMAGE);
		t.invulnerableTime = 0; // тычок не съедает следующий удар клинком
		Vec3 dir = t.position().subtract(p.position()).multiply(1, 0, 1);
		if (dir.lengthSqr() < 1.0e-4) dir = p.getLookAngle().multiply(1, 0, 1);
		dir = dir.normalize();
		t.knockback(interrupted ? KNOCKBACK_INTERRUPT : KNOCKBACK, -dir.x, -dir.z);
		Vec3 at = hit.getLocation();
		if (interrupted) {
			t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STAGGER, 3, false, false));
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.3);
			level.playSound(null, e.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.6f);
			level.playSound(null, e.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 1.6f);
		} else {
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 5, 0.15, 0.15, 0.15, 0.1);
			level.playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8f, 1.2f);
		}
	}

	/** Сервер, ItemStack, Level — для вызова из предмета. */
	public static boolean useFromItem(Level level, Player player, ItemStack stack) {
		return !(player instanceof ServerPlayer sp) || use(sp, stack);
	}
}
