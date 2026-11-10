package com.tarkrakov.tearsofink.world;

import net.minecraft.world.level.biome.Climate;

/** Доступ к раскладке Чернильных Земель у источника биомов (реализует mixin). */
public interface InkLandsAccess {
	/** @return раскладка или null, если это не обычный мир (например, Незер). */
	InkLandsLayout tearsofink$layout(Climate.Sampler sampler);
}
