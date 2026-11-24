package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.hunt.HunterWeaponItem;
import com.tarkrakov.tearsofink.item.Descriptions;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.item.OrdinaryArmorItem;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.weapon.*;
import com.tarkrakov.tearsofink.weapon.blood.BloodAbilities;
import com.tarkrakov.tearsofink.weapon.combat.*;
import com.tarkrakov.tearsofink.weapon.forge.*;
import com.tarkrakov.tearsofink.weapon.shield.ShieldTuning;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.ink.FoodBalance;
import com.tarkrakov.tearsofink.ink.SoulPenalty;
import com.tarkrakov.tearsofink.world.EquipmentAffinity;
import com.tarkrakov.tearsofink.world.VanillaArmorPenalty;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** One native item tooltip, not an overlapping second window. All gameplay stays server-owned. */
public final class WeaponStats {
    private static final String PREFIX = "equipment.tears-of-ink.";
    private static EquipmentInfo.Sync serverInfo;
    private WeaponStats() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(EquipmentInfo.Sync.TYPE, (payload, context) ->
                context.client().execute(() -> serverInfo = payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverInfo = null);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> tooltip(stack, lines));
    }

    private static MutableComponent t(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }
    private static void add(List<Component> out, String key, ChatFormatting color, Object... args) {
        out.add(t(key, args).withStyle(color));
    }
    private static String fmt(double n) {
        if (Math.abs(n) < 0.00001) return "0";
        if (Math.abs(n - Math.rint(n)) < 0.00001) return Long.toString(Math.round(n));
        return String.format(Locale.ROOT, "%.2f", n).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private static String signed(double n) {
        return (n > 0.00001 ? "+" : "") + fmt(n);
    }
    private static String range(double min, double max) {
        return fmt(min).equals(fmt(max)) ? fmt(min) : fmt(min) + "–" + fmt(max);
    }
    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents tc ? tc.getKey() : "";
    }
    private static ItemAttributeModifiers modifiers(ItemStack stack) {
        var m = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return m.modifiers().isEmpty() ? stack.getItem().getDefaultAttributeModifiers() : m;
    }
    private static double amount(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attr) {
        double[] result = {0};
        modifiers(stack).forEach(slot, (attribute, modifier) -> {
            if (attribute.equals(attr)) result[0] += modifier.amount();
        });
        return result[0];
    }
    private static boolean vanilla(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
    }

    private static void tooltip(ItemStack stack, List<Component> lines) {
        Minecraft mc = Minecraft.getInstance();
        boolean inside = mc.level != null && ModDimensions.hasLandsRules(mc.level);
        Item item = stack.getItem();
        if (inside && vanilla(stack) && stack.get(DataComponents.FOOD) != null) {
            lines.add(t("food.foreign", fmt((1 - FoodBalance.VANILLA_IN_LANDS) * 100)).withStyle(ChatFormatting.RED));
        }
        if (inside && stack.is(ModItems.RAW_CATERPILLAR_MEAT)) {
            lines.add(t("food.corrupted", SoulPenalty.RAW_MEAT_STAGE).withStyle(ChatFormatting.GRAY));
        }
        if (!(item instanceof AbilityWeapon) && !(item instanceof OrdinaryArmorItem)) {
            if (inside && !Gear.isModItem(stack)) {
                if (item instanceof ArmorItem) lines.add(t("penalty.armor", fmt((1 - VanillaArmorPenalty.LAND_MULTIPLIER) * 100)).withStyle(ChatFormatting.RED));
                else if (item instanceof TieredItem || item instanceof BowItem || item instanceof CrossbowItem
                        || item instanceof TridentItem || item instanceof MaceItem)
                    lines.add(t("penalty.weapon", fmt((1 - Gear.VANILLA_WEAPON_MULT) * 100)).withStyle(ChatFormatting.RED));
            }
            return;
        }
        // Remove only the mod's own generated descriptions and old equipment rows. Keep name,
        // enchantments, custom lore, advanced ID and other mods' independent details.
        List<Component> legacy = new ArrayList<>();
        Descriptions.add(item.getDescriptionId(), legacy);
        if (Gear.ours(stack)) Awakening.tooltip(stack, legacy);
        lines.removeAll(legacy);
        boolean extraModifiers = modifiers(stack).modifiers().stream().anyMatch(e ->
                !e.attribute().equals(Attributes.ATTACK_DAMAGE) && !e.attribute().equals(Attributes.ATTACK_SPEED)
                && !e.attribute().equals(Attributes.ARMOR) && !e.attribute().equals(Attributes.ARMOR_TOUGHNESS)
                && !e.attribute().equals(Attributes.MOVEMENT_SPEED));
        for (int i = lines.size() - 1; i > 0; i--) {
            String k = key(lines.get(i));
            if (k.equals("gear.tears-of-ink.tier_line") || k.equals("gear.tears-of-ink.sharp_line")
                    || k.equals("item.durability") || (!extraModifiers && k.startsWith("attribute.modifier."))) lines.remove(i);
            else if (!extraModifiers && k.startsWith("item.modifiers.")) {
                lines.remove(i);
                // Two removals in this iteration: advance past the removed spacer as well.
                if (i > 1 && lines.get(i - 1).getString().isEmpty()) lines.remove(--i);
            }
        }

        boolean upgrades = Gear.ours(stack) && Screen.hasControlDown();
        boolean detail = Screen.hasShiftDown() && !upgrades;
        List<Component> out = new ArrayList<>();
        if (item instanceof OrdinaryArmorItem armor) armor(out, stack, armor, detail, inside);
        else weapon(out, stack, (AbilityWeapon) item, detail, upgrades);
        if (mc.level != null && !inside && (item instanceof OrdinaryArmorItem || item instanceof SwordItem || item instanceof SoulShieldItem)) {
            add(out, item instanceof OrdinaryArmorItem ? "penalty.mod_armor" : "penalty.mod_weapon", ChatFormatting.RED,
                    fmt((1 - EquipmentAffinity.MOD_OUTSIDE_LANDS) * 100));
        }
        add(out, detail || upgrades ? "summary_hint" : Gear.ours(stack) ? "views_hint" : item instanceof OrdinaryArmorItem ? "armor_hint" : "details_hint", ChatFormatting.GRAY);
        lines.addAll(Math.min(1, lines.size()), out);
        // Wrap by actual font pixel width, not a count of characters. Native tooltip positioning
        // stays responsible for the cursor and screen edges. Do not shrink the player's font.
        int width = Math.max(48, Math.min(224, mc.getWindow().getGuiScaledWidth() - 28));
        List<Component> wrapped = new ArrayList<>();
        for (Component line : lines) {
            if (mc.font.width(line) <= width) { wrapped.add(line); continue; }
            for (var part : mc.font.getSplitter().splitLines(line, width, Style.EMPTY)) {
                MutableComponent row = Component.empty();
                part.visit((style, value) -> {
                    row.append(Component.literal(value).withStyle(style));
                    return Optional.empty();
                }, Style.EMPTY);
                wrapped.add(row);
            }
        }
        lines.clear();
        lines.addAll(wrapped);
    }

    private static void armor(List<Component> out, ItemStack stack, OrdinaryArmorItem armor, boolean detail, boolean inside) {
        var slot = armor.getEquipmentSlot();
        double defense = amount(stack, slot, Attributes.ARMOR), tough = amount(stack, slot, Attributes.ARMOR_TOUGHNESS);
        double multiplierHere = EquipmentAffinity.multiplier(inside, true);
        defense *= multiplierHere;
        tough *= multiplierHere;
        double speed = amount(stack, slot, Attributes.MOVEMENT_SPEED) * 100;
        add(out, "armor.type", ChatFormatting.GOLD, t("weight." + armor.weightId()), tier(armor.tier(stack).ordinal()));
        add(out, "armor.defense", ChatFormatting.WHITE, fmt(defense));
        add(out, "armor.toughness", ChatFormatting.WHITE, fmt(tough));
        add(out, "armor.speed", speed < -0.00001 ? ChatFormatting.RED : speed > 0.00001 ? ChatFormatting.GREEN : ChatFormatting.GRAY, signed(speed));
        durability(out, stack);
        if (detail) {
            add(out, "armor.role." + armor.weightId(), ChatFormatting.GRAY);
            add(out, "armor.toughness_help", ChatFormatting.GRAY);
            var player = Minecraft.getInstance().player;
            ItemStack worn = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
            if (!worn.isEmpty() && worn != stack && worn.getItem() instanceof ArmorItem) {
                double multiplier = EquipmentAffinity.multiplier(inside, Gear.isModItem(worn));
                add(out, "armor.compare", ChatFormatting.YELLOW,
                        signed(defense - amount(worn, slot, Attributes.ARMOR) * multiplier),
                        signed(tough - amount(worn, slot, Attributes.ARMOR_TOUGHNESS) * multiplier),
                        signed(speed - amount(worn, slot, Attributes.MOVEMENT_SPEED) * 100));
            }
        }
    }

    private static Component tier(int n) {
        return Component.translatable("gear.tears-of-ink.tier." + n);
    }
    private static void durability(List<Component> out, ItemStack stack) {
        if (!stack.isDamageableItem()) return;
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        add(out, "durability", remaining <= stack.getMaxDamage() / 4 ? ChatFormatting.GOLD : ChatFormatting.GRAY,
                remaining, stack.getMaxDamage());
    }

    private static void weapon(List<Component> out, ItemStack stack, AbilityWeapon weapon, boolean detail, boolean upgrades) {
        Item item = stack.getItem();
        boolean gear = Gear.ours(stack), ordinary = item instanceof OrdinaryWeaponItem, shield = item instanceof SoulShieldItem;
        boolean two = InkGreatswordItem.twoHanded(stack);
        Archetype archetype = Archetype.of(stack);
        if (shield) {
            SoulShieldItem sh = (SoulShieldItem)item;
            add(out, "shield.type", ChatFormatting.AQUA, fmt(sh.power() * 100));
            add(out, "shield.family." + switch(sh.profile()) { case RAM, REPEL -> "attack"; case LIGHT -> "defense"; case MERCY -> "support"; }, ChatFormatting.GOLD);
            if (detail) add(out, "shield.profile", ChatFormatting.AQUA, fmt(sh.profile().blockCost * 100), fmt(sh.profile().cover * 2));
        }
        else if (gear) {
            Component kind = t(ordinary ? "class.ordinary" : item instanceof HunterWeaponItem || item instanceof ArsenalWeaponItem
                    || stack.is(ModItems.CLEAVE_GREATSWORD) || stack.is(ModItems.WAVE_GREATSWORD) ? "class.hunter" : "class.other");
            add(out, "weapon.type", ChatFormatting.GOLD, kind, t(two ? "hands.two" : "hands.one"));
            var line = item instanceof OrdinaryWeaponItem o ? o.line()
                    : item instanceof ArsenalWeaponItem a ? a.line()
                    : item instanceof HunterWeaponItem ? HunterWeaponItem.line(stack) : null;
            add(out, "weapon.tier", ChatFormatting.GRAY, tier(Gear.tier(stack)),
                    line == null ? t("line.none") : t("line." + line.id));
        } else add(out, item instanceof SwordItem ? "class.knife" : "class.pendant", ChatFormatting.AQUA);

        if (upgrades) {
            int cap = Gear.SHARP_MAX[Gear.tier(stack)];
            if (cap == 0) add(out, "sharp.none", ChatFormatting.GRAY);
            else {
                add(out, "sharp.value", ChatFormatting.WHITE, Gear.sharp(stack), cap, Math.round(Gear.sharpFraction(stack)*100));
                add(out, "sharp.wear", ChatFormatting.GRAY, Gear.WEAR_HIT, Gear.WEAR_ABILITY);
                add(out, "sharp.degrade", ChatFormatting.GRAY, fmt(Gear.DEGRADE_AT * 100));
            }
            awakenings(out, stack, true);
            return;
        }
        if (!detail && (item instanceof SwordItem || shield)) {
            double damage = 1 + amount(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
            double charge = 4 + amount(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED);
            if (shield) add(out, "shield.bash", ChatFormatting.WHITE, fmt(damage), fmt(charge));
            else {
                double base = damage;
                if (gear) damage *= Gear.TIER_MULT[Gear.tier(stack)];
                add(out, gear ? "weapon.damage_lands" : "weapon.damage", ChatFormatting.WHITE, fmt(damage));
                add(out, "weapon.recharge", ChatFormatting.WHITE, fmt(charge));
                if (detail && gear && Gear.tier(stack) != 0) add(out, "weapon.before_tier", ChatFormatting.GRAY, fmt(base), fmt((Gear.TIER_MULT[Gear.tier(stack)]-1)*100));
            }
        }

        if (!detail && ordinary) {
            var o = (OrdinaryWeaponItem) item;
            add(out, "passive.ink", ChatFormatting.LIGHT_PURPLE, fmt(Gear.ORDINARY_COMBO_INK));
            if (o.line() == com.tarkrakov.tearsofink.hunt.HunterType.BLOOD)
                add(out, "passive.bleed", ChatFormatting.RED, fmt(Gear.ORDINARY_BLEED_CHANCE*100));
            else if (o.line() == com.tarkrakov.tearsofink.hunt.HunterType.INK)
                add(out, "passive.slow", ChatFormatting.LIGHT_PURPLE, fmt(Gear.INK_SLOW_TICKS/20.0));
        } else if (!detail && gear) {
            add(out, "passive.no_ink", ChatFormatting.GRAY);
            if (archetype != null && item instanceof ArsenalWeaponItem a && a.blood())
                add(out, "passive.bleed", ChatFormatting.RED, fmt(com.tarkrakov.tearsofink.weapon.blood.BloodTuning.BLEED_CHANCE*100));
        }

        if (!detail && gear) {
            int cap = Gear.SHARP_MAX[Gear.tier(stack)];
            if (cap == 0) add(out, "sharp.none", ChatFormatting.GRAY);
            else add(out, "sharp.value", Gear.sharpFraction(stack) <= Gear.WARN_AT ? ChatFormatting.GOLD : ChatFormatting.GRAY,
                    Gear.sharp(stack), cap, Math.round(Gear.sharpFraction(stack)*100));
            awakenings(out, stack, false);
        } else if (!detail) durability(out, stack);

        Skill skill = weapon.ability();
        if (stack.is(ModItems.PALE_LINEAR_GREATSWORD)) {
            add(out, "pale.primary", ChatFormatting.AQUA, SkillKeys.weaponKeyLabel());
            if (serverInfo != null) add(out, "pale.cooldown", ChatFormatting.GRAY,
                    cooldown(stack, serverInfo.pale().singleR()), cooldown(stack, serverInfo.pale().dualR()));
            if (detail) {
                add(out, "pale.guard", ChatFormatting.GRAY, Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage());
                add(out, "pale.pair", ChatFormatting.AQUA, SkillKeys.weaponKeyLabel(), Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage());
                if (serverInfo != null) add(out, "pale.guard_values", ChatFormatting.GRAY,
                        fmt(serverInfo.pale().singleGuard()*100), fmt(serverInfo.pale().dualGuard()*100),
                        cooldown(stack, serverInfo.pale().charge()));
            }
        } else if (stack.is(ModItems.PALE_LONGSWORD)) {
            add(out, "longsword.primary", ChatFormatting.AQUA, SkillKeys.weaponKeyLabel());
            var single = com.tarkrakov.tearsofink.weapon.combat.PaleLongswordTuning.SINGLE;
            var pair = com.tarkrakov.tearsofink.weapon.combat.PaleLongswordTuning.DUAL;
            add(out, "longsword.cooldown", ChatFormatting.GRAY, cooldown(stack, single.cooldown()), cooldown(stack, pair.cooldown()));
            if (detail) {
                add(out, "longsword.pair", ChatFormatting.AQUA, SkillKeys.weaponKeyLabel(), Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage());
                add(out, "longsword.guard", ChatFormatting.GRAY, fmt(com.tarkrakov.tearsofink.weapon.combat.PaleLongswordTuning.BLOCK_REDUCTION*100), cooldown(stack, com.tarkrakov.tearsofink.weapon.combat.PaleLongswordTuning.BLOCK_COOLDOWN));
            }
        } else if (skill != null) {
            Component control = item instanceof SalvationPendantItem ? t("offkey", SkillKeys.weaponKeyLabel()) : SkillKeys.weaponKeyLabel();
            skill(out, stack, skill, control, detail);
        } else if (!ordinary && detail) add(out, "ability.none", ChatFormatting.GRAY);

        if (detail) {
            if (archetype != null) {
                if (Language.getInstance().has(PREFIX + "role." + archetype.id)) add(out, "role." + archetype.id, ChatFormatting.GRAY);
                if (shield) {
                    combo(out, archetype, true);
                    if (view(archetype, true) == null) combo(out, archetype, false);
                } else {
                    combo(out, archetype, false);
                    combo(out, archetype, true);
                }
            }
            if (shield) {
                add(out, "shield.stance", ChatFormatting.AQUA, fmt(ShieldTuning.PERFECT_TICKS/20.0));
                add(out, "shield.resonance", ChatFormatting.GRAY, SkillKeys.weaponKeyLabel(), ShieldTuning.CHARGE_MAX);
                add(out, "shield.resonance_cost", ChatFormatting.AQUA,
                        com.tarkrakov.tearsofink.weapon.shield.ResonanceTuning.SOUL_COST,
                        fmt(com.tarkrakov.tearsofink.weapon.shield.ResonanceTuning.COOLDOWN_TICKS/20.0));
                add(out, "shield.paired", ChatFormatting.GRAY);
                add(out, "shield.phantom", ChatFormatting.GRAY, fmt(ShieldTuning.PHANTOM_RANGE), fmt(ShieldTuning.PHANTOM_DAMAGE_MULT * 100));
                add(out, "shield.offhand", ChatFormatting.GRAY, t("offkey", SkillKeys.weaponKeyLabel()));
            } else if (gear && archetype != null) {
                if (!two) add(out, "hilt", ChatFormatting.GRAY, Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(), fmt(HiltStrike.COOLDOWN/20.0));
                else if (!stack.is(ModItems.PALE_LINEAR_GREATSWORD)) add(out, "twohand", ChatFormatting.GRAY);
                if (!ordinary) {
                    Skill pair = switch (archetype) {
                        case EXECUTIONER -> WeaponAbilities.GUILLOTINE;
                        case DAGGER -> DaggerMoves.DUAL_RUSH;
                        case AXE -> AxeMoves.EXECUTION;
                        case TALWAR -> BloodAbilities.TALWAR_CROSS;
                        case DUSSAK -> BloodAbilities.DOUBLE_PARRY;
                        default -> WeaponAbilities.FLOURISH;
                    };
                    skill(out, stack, pair, t("pairkey", Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage()), false);
                }
            }
        }
    }

    private static String cooldown(ItemStack stack, int ticks) {
        return fmt(Math.round(ticks * Awakening.cooldownMult(stack)) / 20.0);
    }
    private static void skill(List<Component> out, ItemStack stack, Skill skill, Component control, boolean detail) {
        add(out, "ability.name", ChatFormatting.AQUA, control, Component.translatable(skill.nameKey()));
        List<Component> values = new ArrayList<>();
        float cost = Awakening.costMult(stack);
        if (skill.inkCost() > 0) values.add(t("cost.ink", fmt(skill.inkCost()*cost)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (skill.soulCost() > 0) values.add(t("cost.soul", skill.soulCost()).withStyle(ChatFormatting.AQUA));
        if (skill.healthCost() > 0) values.add(t("cost.health", fmt(skill.healthCost()*cost)).withStyle(ChatFormatting.RED));
        if (values.isEmpty()) values.add(t("cost.free").withStyle(ChatFormatting.GRAY));
        if (skill == WeaponAbilities.MERCY) values.add(t("cost.drain", fmt(WeaponAbilities.MercyAbility.SOUL_PER_SECOND)).withStyle(ChatFormatting.AQUA));
        if (skill.cooldownTicks() > 0 && skill != WeaponAbilities.MERCY)
            values.add(t("cooldown", cooldown(stack, skill.cooldownTicks())).withStyle(ChatFormatting.GRAY));
        MutableComponent row = Component.empty();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) row.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            row.append(values.get(i));
        }
        out.add(row);
        if (detail) {
            String id = skill.id().getPath();
            if (Language.getInstance().has(PREFIX + "move." + id)) add(out, "move." + id, ChatFormatting.GRAY);
            if (skill == WeaponAbilities.MERCY) add(out, "mercy.lockout", ChatFormatting.GRAY, fmt(WeaponAbilities.MercyAbility.LOCKOUT_TICKS / 20.0));
        }
    }

    private static void awakenings(List<Component> out, ItemStack stack, boolean detail) {
        List<Awakening.Slot> slots = Awakening.read(stack);
        int cap = Awakening.slots(stack), active = 0, dormant = 0;
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot == null) continue;
            if (i < cap && Awakening.allowedOn(stack, slot.awakening())) active++;
            else dormant++;
        }
        add(out, "awakening.count", ChatFormatting.LIGHT_PURPLE, active, cap);
        if (dormant > 0 && !detail) add(out, "awakening.dormant", ChatFormatting.GRAY, dormant);
        if (!detail) return;
        // Names and effective values are separated from attack statistics.
        List<Component> named = new ArrayList<>();
        Awakening.tooltip(stack, named);
        for (Component line : named) if (!key(line).equals("awakening.tears-of-ink.empty")) out.add(line);
        if (detail) {
            for (Awakening a : Awakening.values()) {
                float strength = Awakening.power(stack, a);
                if (strength > 0) add(out, "awakening." + a.id, ChatFormatting.GRAY,
                        fmt(a == Awakening.VISCOUS ? strength / 20.0 : strength * 100));
            }
        }
    }

    private static EquipmentInfo.ComboView view(Archetype a, boolean pair) {
        if (serverInfo == null) return null;
        for (var c : serverInfo.combos()) if (c.id().equals(a.id) && c.paired() == pair) return c;
        return null;
    }
    private static void combo(List<Component> out, Archetype a, boolean pair) {
        var v = view(a, pair);
        if (v == null) return;
        add(out, pair ? "combo.pair" : "combo.single", ChatFormatting.WHITE, v.steps(), range(v.minMultiplier(),v.maxMultiplier()));
        add(out, "combo.reach", ChatFormatting.GRAY, range(v.minReach(),v.maxReach()), range(v.minTicks()/20.0,v.maxTicks()/20.0));
    }
}
