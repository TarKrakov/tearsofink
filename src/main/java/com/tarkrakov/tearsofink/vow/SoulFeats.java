package com.tarkrakov.tearsofink.vow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.advancement.ModAdvancements;
import com.tarkrakov.tearsofink.hunt.HunterEntity;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * Повторяемые боевые действия накапливают ПРОГРЕСС, а Душа выдаётся только за конечные Подвиги.
 * Прогресс сохранён отдельным attachment (старый VowData и миры остаются совместимыми).
 */
public final class SoulFeats {
	public static final float FIRST_BLOCK = 100f, STOICISM = 1000f;
	public static final int FIRST_PERFECT = 10, MASTER_PERFECT = 50;
	public static final int FIRST_KILLS = 25, HUNDRED_KILLS = 100;
	/** Десять полных Резонансов дают доступную одиночному игроку десятую награду +5 Души. */
	public static final int MASTER_RESONANCES = 10;
	public static final int REWARD = 5;

	public record Progress(float blocked, int perfect, int kills, int resonances) {
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.FLOAT.optionalFieldOf("blocked", 0f).forGetter(Progress::blocked),
				Codec.INT.optionalFieldOf("perfect", 0).forGetter(Progress::perfect),
				Codec.INT.optionalFieldOf("kills", 0).forGetter(Progress::kills),
				Codec.INT.optionalFieldOf("resonances", 0).forGetter(Progress::resonances)
		).apply(i, Progress::new));
	}

	public static final AttachmentType<Progress> DATA = AttachmentRegistry.<Progress>builder()
			.persistent(Progress.CODEC).copyOnDeath()
			.initializer(() -> new Progress(0f, 0, 0, 0))
			.buildAndRegister(TearsOfInk.id("soul_feats"));

	private SoulFeats() {}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(SoulFeats::onDeath);
	}

	/** Не засчитывать самоповреждение, союзников, пассивных животных и творческий режим. */
	public static boolean isEnemy(ServerPlayer player, Entity attacker) {
		return attacker != null && attacker != player && !attacker.isAlliedTo(player)
				&& (attacker instanceof Monster || attacker instanceof HunterEntity);
	}

	public static void onBlock(ServerPlayer player, DamageSource source, float damage, boolean perfect) {
		if (Vows.vow(player) != Vow.SOUL || !InkPowers.get(player).inside()
				|| player.getAbilities().instabuild || damage <= 0 || !isEnemy(player, source.getEntity())) return;
		Progress old = player.getAttachedOrCreate(DATA);
		Progress next = new Progress(Math.min(STOICISM, old.blocked() + damage),
				Math.min(MASTER_PERFECT, old.perfect() + (perfect ? 1 : 0)), old.kills(), old.resonances());
		player.setAttached(DATA, next);
		if (old.blocked() < FIRST_BLOCK && next.blocked() >= FIRST_BLOCK)
			Vows.awaken(player, "feat_block_100", REWARD);
		if (old.blocked() < STOICISM && next.blocked() >= STOICISM) {
			Vows.awaken(player, "feat_block_1000", REWARD);
			ModAdvancements.grant(player, "soul_stoicism");
		}
		if (old.perfect() < FIRST_PERFECT && next.perfect() >= FIRST_PERFECT)
			Vows.awaken(player, "feat_perfect_10", REWARD);
		if (old.perfect() < MASTER_PERFECT && next.perfect() >= MASTER_PERFECT)
			Vows.awaken(player, "feat_perfect_50", REWARD);
	}

	/** Отдельный конечный Подвиг: десять полностью заряженных Резонансов. */
	public static void onChargedResonance(ServerPlayer player, int charge) {
		if (charge < 5 || Vows.vow(player) != Vow.SOUL || !InkPowers.get(player).inside()
				|| player.getAbilities().instabuild) return;
		Progress old = player.getAttachedOrCreate(DATA);
		if (old.resonances() >= MASTER_RESONANCES) return;
		int count = old.resonances() + 1;
		player.setAttached(DATA, new Progress(old.blocked(), old.perfect(), old.kills(), count));
		if (count == MASTER_RESONANCES) Vows.awaken(player, "feat_resonance_10", REWARD);
	}

	/** Кооперативная, необязательная награда — лечение другого игрока при здоровье ниже сердца. */
	public static void onEmergencyHeal(ServerPlayer healer, ServerPlayer ally, float before) {
		if (healer == ally || before >= 2f || ally.getHealth() <= before
				|| Vows.vow(healer) != Vow.SOUL || !InkPowers.get(healer).inside()
				|| healer.getAbilities().instabuild) return;
		if (Vows.awaken(healer, "feat_emergency_heal", REWARD))
			ModAdvancements.grant(healer, "soul_lie_down");
	}

	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player)
				|| source.getDirectEntity() != player
				|| victim == player || victim.isAlliedTo(player)
				|| !(victim instanceof Monster || victim instanceof HunterEntity)
				|| !(player.getMainHandItem().getItem() instanceof SoulShieldItem)
				|| Vows.vow(player) != Vow.SOUL || !InkPowers.get(player).inside()
				|| player.getAbilities().instabuild) return;
		Progress old = player.getAttachedOrCreate(DATA);
		Progress next = new Progress(old.blocked(), old.perfect(), Math.min(HUNDRED_KILLS, old.kills() + 1), old.resonances());
		player.setAttached(DATA, next);
		if (old.kills() < FIRST_KILLS && next.kills() >= FIRST_KILLS)
			Vows.awaken(player, "feat_shield_kills_25", REWARD);
		if (old.kills() < HUNDRED_KILLS && next.kills() >= HUNDRED_KILLS) {
			Vows.awaken(player, "feat_shield_kills_100", REWARD);
			ModAdvancements.grant(player, "soul_hundred_kills");
		}
	}
}
