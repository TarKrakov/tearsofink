package com.tarkrakov.tearsofink.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Навык, который игрок выучивает из книги. Рывок — первый; дальше добавляются новые наследники.
 * <p>
 * Активный навык запускается клавишей ({@link #activate}); цену (Чернила/Душа), перезарядку
 * и проверку «внутри Чернильных Земель» берёт на себя {@link Skills}. Пассивный навык
 * просто проверяется через {@link Skills#knows}.
 */
public abstract class Skill {
	private final ResourceLocation id;
	private final int inkCost;
	private final int soulCost;
	private final int cooldownTicks;

	protected Skill(ResourceLocation id, int inkCost, int soulCost, int cooldownTicks) {
		this.id = id;
		this.inkCost = inkCost;
		this.soulCost = soulCost;
		this.cooldownTicks = cooldownTicks;
	}

	public ResourceLocation id() {
		return id;
	}

	public int inkCost() {
		return inkCost;
	}

	public int soulCost() {
		return soulCost;
	}

	public int cooldownTicks() {
		return cooldownTicks;
	}

	/** Цена здоровьем (оружие Крови платит собой). 0 — не платит. */
	public float healthCost() {
		return 0;
	}

	/** Есть ли у навыка клавиша (активный) или он работает сам (пассивный). */
	public boolean isActive() {
		return true;
	}

	/** Ключ перевода названия: {@code skill.tears-of-ink.<имя>}. */
	public String nameKey() {
		return "skill." + id.getNamespace() + "." + id.getPath();
	}

	/**
	 * Выполнить навык. Вызывается на сервере, когда все проверки пройдены.
	 * @return {@code true}, если сработал — тогда спишется цена и начнётся перезарядка
	 */
	public abstract boolean activate(ServerPlayer player, SkillInput input);
}
