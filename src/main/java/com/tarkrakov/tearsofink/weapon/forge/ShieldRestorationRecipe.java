package com.tarkrakov.tearsofink.weapon.forge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * Restore the pilgrim's starter equipment at an ordinary crafting table.
 * ShapelessRecipe exposes the actual ingredients and result to the recipe book,
 * diary links and JEI. Assembly preserves personal components but takes the
 * restored item's own attributes, durability and other mechanical defaults. The serializer ID is retained for compatibility.
 */
public final class ShieldRestorationRecipe extends ShapelessRecipe {
    public static final MapCodec<ShieldRestorationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ItemStack.STRICT_CODEC.fieldOf("worn").forGetter(r -> r.worn),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.restored)
    ).apply(i, ShieldRestorationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldRestorationRecipe> STREAM_CODEC =
        StreamCodec.composite(ItemStack.STREAM_CODEC, r -> r.worn, ItemStack.STREAM_CODEC, r -> r.restored,
            ShieldRestorationRecipe::new);

    private final ItemStack worn;
    private final ItemStack restored;

    public ShieldRestorationRecipe(ItemStack worn, ItemStack restored) {
        super("", CraftingBookCategory.EQUIPMENT, restored,
            NonNullList.of(Ingredient.EMPTY, Ingredient.of(worn.getItem()), Ingredient.of(ModItems.PALE_INGOT)));
        if (worn.getCount() != 1 || restored.getCount() != 1 ||
            !(worn.is(ModItems.WORN_RAM_SHIELD) && restored.is(ModItems.RAM_SHIELD)
              || worn.is(ModItems.WORN_MERCY_SHIELD) && restored.is(ModItems.MERCY_SHIELD)
              || worn.is(ModItems.DIM_SCIMITAR) && restored.is(ModItems.WAVE_GREATSWORD)))
            throw new IllegalArgumentException("Invalid starter equipment restoration pair");
        this.worn = worn.copy();
        this.restored = restored.copy();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = restored.copy();
        for (int i = 0; i < input.size(); i++) {
            ItemStack source = input.getItem(i);
            if (!source.is(worn.getItem())) continue;
            copy(source, out, DataComponents.CUSTOM_NAME);
            copy(source, out, DataComponents.ENCHANTMENTS);
            copy(source, out, DataComponents.LORE);
            copy(source, out, DataComponents.BANNER_PATTERNS);
            copy(source, out, DataComponents.BASE_COLOR);
            copy(source, out, DataComponents.REPAIR_COST);
            copy(source, out, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            copy(source, out, DataComponents.UNBREAKABLE);
            copy(source, out, DataComponents.CUSTOM_DATA);
            out.setDamageValue(0);
            return out;
        }
        return ItemStack.EMPTY;
    }

    private static <T> void copy(ItemStack source, ItemStack out, DataComponentType<T> type) {
        T value = source.get(type);
        if (value != null) out.set(type, value);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModForge.SHIELD_RESTORATION;
    }
}
