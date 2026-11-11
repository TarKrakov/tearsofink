package com.tarkrakov.tearsofink.weapon;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Материал оружия мода — Асфода (чинится Асфодой на наковальне). */
public enum ModTiers implements Tier {
	ASPHODA(600, 7f, 2f, 14);

	private final int uses;
	private final float speed;
	private final float damage;
	private final int enchantment;

	ModTiers(int uses, float speed, float damage, int enchantment) {
		this.uses = uses;
		this.speed = speed;
		this.damage = damage;
		this.enchantment = enchantment;
	}

	@Override public int getUses() { return uses; }
	@Override public float getSpeed() { return speed; }
	@Override public float getAttackDamageBonus() { return damage; }
	@Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
	@Override public int getEnchantmentValue() { return enchantment; }
	@Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.ASPHODA); }
}
