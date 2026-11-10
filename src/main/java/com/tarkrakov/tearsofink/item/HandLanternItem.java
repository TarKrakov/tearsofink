package com.tarkrakov.tearsofink.item;

import net.minecraft.world.item.Item;

/**
 * Ручной чернильный фонарь: рассеивает мглу вокруг игрока.
 * В левой руке — полная сила, в хотбаре — слабее (см. клиентский туман).
 * Не светит и не защищает от врагов — только видимость.
 */
public class HandLanternItem extends Item {
	/** На сколько блоков видно в тумане с этим фонарём в левой руке. */
	private final float viewDistance;

	public HandLanternItem(float viewDistance, Properties properties) {
		super(properties);
		this.viewDistance = viewDistance;
	}

	public float viewDistance() {
		return viewDistance;
	}
}
