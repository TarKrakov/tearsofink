package com.tarkrakov.tearsofink.skill;

/**
 * Что игрок нажимал в момент навыка.
 * @param strafe  влево (+1) / вправо (−1), как у ванильного движения
 * @param forward вперёд (+1) / назад (−1)
 * @param sneaking зажат ли Shift (для будущих навыков «левой руки»)
 */
public record SkillInput(float strafe, float forward, boolean sneaking) {}
