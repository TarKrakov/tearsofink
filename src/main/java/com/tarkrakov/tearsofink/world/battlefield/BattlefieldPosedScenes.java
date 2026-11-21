package com.tarkrakov.tearsofink.world.battlefield;

import java.util.List;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

/**
 * D-123: meaningfully POSED scene, not just cardinal directions of block models.
 * All measurements are Minecraft blocks relative to the same local episode
 * axis: positive forward = toward the front of the scene, positive right = right.
 * Angles are actual independent Euler angles in degrees, applied Y -> Z -> X
 * around the model's *ground contact*, not Minecraft blockstate rotations.
 * Banners remain the author's animated blocks; only static props are posed.
 *
 * This is a temporary small-scene preview. Massive fields will require an
 * author-approved static baking strategy, not thousands of rendered block entities.
 */
public final class BattlefieldPosedScenes {
    private BattlefieldPosedScenes() {}

    /**
     * Persistent IDs: do not reorder. Scene 0/1 preserve existing saved
     * battlefield_scene blockstates and their authored preview positions.
     * Additional scenes are technical composition motifs, not historical lore.
     */
    public enum Scenario {
        LAST_STAND("last_stand", "Последний рубеж"),
        BANNER_LINE("banner_line", "Знамённая линия"),
        BROKEN_WALL("broken_wall", "Разбитая оборона"),
        SPEAR_COLLAPSE("spear_collapse", "Павший копейный строй"),
        ABANDONED_POST("abandoned_post", "Брошенная позиция"),
        BLOOD_DUEL("blood_duel", "Схватка с тальварами"),
        SPIRE_CLASH("spire_clash", "Тяжёлое столкновение"),
        HUNTER_TRACE("hunter_trace", "След охотников"),
        PALE_OATH("pale_oath", "Последние два клинка"),
        BLADE_GROVE("blade_grove", "Неровная роща клинков"),
        FALLEN_EDGE("fallen_edge", "Поваленный рубеж"),
        TWIN_STANDARDS("twin_standards", "Рваная знамённая дуга"),
        CROSS_FIRE("cross_fire", "След перекрёстной стрельбы"),
        FINAL_TRACE("final_trace", "Далёкие следы боя"),
        SHIELD_CRESCENT("shield_crescent", "Разорванный полумесяц щитов"),
        KNIFE_SCATTER("knife_scatter", "Рассыпанные короткие клинки"),
        AXE_BREACH("axe_breach", "Брешь между секирами"),
        BLADE_FAN("blade_fan", "Веер оставленных мечей"),
        CROSSBOW_CACHE("crossbow_cache", "Брошенное стрелковое гнездо"),
        LONE_BLADE("lone_blade", "Одинокий большой клинок"),
        // D-131: append new IDs only; original scene 0..19 / layouts 0 and 1 remain intact.
        REGULAR_SQUAD_LINE_ANCHOR("regular_squad_line_anchor", "Рубеж регулярной армии"),
        REGULAR_SQUAD_RALLY_POINT("regular_squad_rally_point", "Сбор у отрядного знамени"),
        REGULAR_BARE_REMNANT("regular_bare_remnant", "Сорванное знамя Шпиля"),
        PALADIN_HOLDOUT_ANCHOR("paladin_holdout_anchor", "Опорный рубеж паладинов (без флага)"),
        PALADIN_BARE_RELIC("paladin_bare_relic", "След угасающей звезды (без флага)"),
        HUNTER_STRIKE_MARK("hunter_strike_mark", "След удара охотников (без флага)"),
        HUNTER_BARE_TRACE("hunter_bare_trace", "Последний след охотников (без флага)"),
        LINE_COLLAPSE("line_collapse", "Обрушение линии"),
        GRIND_CENTER("grind_center", "Узел ближнего боя"),
        BREAKTHROUGH_LANE("breakthrough_lane", "Коридор прорыва"),
        AFTERMATH_SCATTER("aftermath_scatter", "Осколки сражения"),
        REAR_GUARD_FALLBACK("rear_guard_fallback", "Прикрытие отступления"),
        ISOLATED_LAST_STAND_SMALL("isolated_last_stand_small", "Одинокий заслон"),
        MIXED_DEAD_GROUND("mixed_dead_ground", "Молчаливое поле");

        private final String id, label;
        Scenario(String id, String label) { this.id = id; this.label = label; }
        public String id() { return id; }
        public String label() { return label; }
    }

    public record Pose(Prop prop, float forward, float right, float sink,
                       float yaw, float pitch, float roll, boolean required) {
        public boolean banner() { return prop == Prop.BANNER || prop == Prop.BANNER_BARE; }
    }

    /**
     * Version 0: EXACT original geometry of D-125 for existing save blocks.
     * Version 1: revised archetypes. An absent layout blockstate property in
     * old chunks resolves to 0, so existing scenes don't change positions.
     */
    @SuppressWarnings("unchecked")
    private static List<Pose>[] buildCache(int layoutVersion) {
        List<Pose>[] cached = (List<Pose>[]) new List<?>[Scenario.values().length * 8];
        for (Scenario scenario : Scenario.values()) {
            for (int variant = 0; variant < 8; variant++) {
                List<Pose> source = scenario.ordinal() >= 20
                        ? BattlefieldHistoricalScenes.plan(scenario, variant)
                        : layoutVersion == 0 && scenario.ordinal() < 14
                                ? buildLegacyPlan(scenario, variant)
                                : BattlefieldCompositionArchetypes.plan(scenario, variant);
                cached[scenario.ordinal() * 8 + variant] = source.stream().map(p -> {
                    if (!BattlefieldWeaponCatalog.isWeapon(p.prop())) return p;
                    var weapon = BattlefieldWeaponCatalog.fromProp(p.prop());
                    return new Pose(p.prop(), p.forward(), p.right(),
                            weapon.groundSink(p.sink(), p.pitch(), p.roll()),
                            p.yaw(), p.pitch(), p.roll(), p.required());
                }).toList();
            }
        }
        return cached;
    }

    private static final List<Pose>[] LEGACY = buildCache(0);
    private static final List<Pose>[] CURRENT = buildCache(1);
    // D-131 scene IDs 20..33 are new, but layout 2 is an explicit save marker.
    // In the renderer all nonzero layouts use CURRENT; IDs 0..19 are immutable.

    public static List<Pose> plan(Scenario scene, int variant) {
        return plan(scene, variant, 1);
    }

    public static List<Pose> plan(Scenario scene, int variant, int layoutVersion) {
        return (layoutVersion == 0 && scene.ordinal() < 20 ? LEGACY : CURRENT)
                [scene.ordinal() * 8 + (variant & 7)];
    }

    private static List<Pose> buildLegacyPlan(Scenario scene, int variant) {
        int mirror = (variant & 1) == 0 ? 1 : -1;
        float skew = (variant & 2) == 0 ? -0.16f : 0.16f;
        float broken = (variant & 4) == 0 ? -4f : 4f;
        boolean collapsed = (variant & 4) != 0;
        return switch (scene) {
            case LAST_STAND -> List.of(
                    // The shield is no longer upright: sunken and bent back.
                    new Pose(Prop.SHIELD, 0f, 0f, -0.29f,
                            (18f + broken) * mirror, -9f, -19f * mirror, true),
                    // Leaning spear top reaches the shield: a genuine relationship.
                    new Pose(Prop.SPEAR, 0.48f, (-0.54f + skew) * mirror, -0.10f,
                            -10f * mirror, 24f, -31f * mirror, true),
                    // A second spear lies at a steep angle across the line.
                    new Pose(Prop.SPEAR, -0.72f, 1.22f * mirror, -0.15f,
                            56f * mirror, 69f, 13f * mirror, false),
                    // A thrown crossbow, close to the shield but not another post.
                    new Pose(Prop.CROSSBOW, 1.05f, 0.74f * mirror, -0.13f,
                            (74f + broken) * mirror, 16f, -12f * mirror, false),
                    // The banner marks the flank, not the centre of the pile.
                    new Pose(Prop.BANNER, -1.30f, -4.25f * mirror, 0f,
                            0f, 0f, 0f, false)
            );
            case BANNER_LINE -> List.of(
                    // The two banners trace a broken diagonal with a traversable gap.
                    new Pose(Prop.BANNER, -1.05f, -4.5f * mirror, 0f,
                            0f, 0f, 0f, true),
                    new Pose(Prop.BANNER, 1.15f, 4.5f * mirror, 0f,
                            0f, 0f, 0f, true),
                    // Shield and spear describe what happened along that line.
                    new Pose(Prop.SHIELD, 0.28f, -0.18f * mirror, -0.36f,
                            (-26f + broken) * mirror, 12f, 27f * mirror, true),
                    new Pose(Prop.SPEAR, 0.75f, (0.74f + skew) * mirror, -0.10f,
                            -30f * mirror, 42f, 31f * mirror, true)
            );

            // A fractured shield curve: the missing central defense is the story.
            case BROKEN_WALL -> List.of(
                    new Pose(Prop.SHIELD, -0.2f, -1.95f * mirror, -0.24f,
                            17f * mirror, -13f, -25f * mirror, true),
                    new Pose(Prop.SHIELD, 0.18f, 0.0f, -0.43f,
                            -31f * mirror, collapsed ? 37f : 10f, 37f * mirror, true),
                    new Pose(Prop.SHIELD, -0.45f, 2.08f * mirror, -0.31f,
                            26f * mirror, 11f, 32f * mirror, false),
                    new Pose(Prop.SPEAR, 0.46f, (-0.35f + skew) * mirror, -0.16f,
                            -14f * mirror, 46f, -27f * mirror, true),
                    new Pose(Prop.SPEAR, -0.65f, 1.7f * mirror, -0.13f,
                            50f * mirror, collapsed ? 76f : 51f, 19f * mirror, false),
                    new Pose(Prop.PALE_LONGSWORD, 0.75f, -1.18f * mirror, -0.16f,
                            38f * mirror, 66f, -14f * mirror, false)
            );
            // Collapse has a directional rhythm of three crossed spears.
            case SPEAR_COLLAPSE -> List.of(
                    new Pose(Prop.SHIELD, -0.8f, -0.75f * mirror, -0.38f,
                            -24f * mirror, 16f, 31f * mirror, true),
                    new Pose(Prop.SPEAR, -0.18f, -0.23f * mirror, -0.11f,
                            28f * mirror, 72f, -11f * mirror, true),
                    new Pose(Prop.SPEAR, 0.42f, 0.49f * mirror, -0.14f,
                            -43f * mirror, collapsed ? 81f : 58f, 18f * mirror, true),
                    new Pose(Prop.SPEAR, -0.64f, 1.22f * mirror, -0.17f,
                            34f * mirror, 30f, -24f * mirror, false),
                    new Pose(Prop.CROSSBOW, 0.91f, 1.66f * mirror, -0.12f,
                            66f * mirror, 77f, 11f * mirror, false)
            );
            // The empty ground behind the shield reads as retreat, not noise.
            case ABANDONED_POST -> List.of(
                    new Pose(Prop.SHIELD, -0.42f, -0.36f * mirror, -0.38f,
                            37f * mirror, 20f, 34f * mirror, true),
                    new Pose(Prop.CROSSBOW, 0.43f, (-0.19f + skew) * mirror, -0.12f,
                            -55f * mirror, 73f, 12f * mirror, true),
                    new Pose(Prop.BLOOD_DUSSAK, 0.85f, 0.60f * mirror, -0.22f,
                            23f * mirror, 85f, -19f * mirror, true),
                    new Pose(Prop.SPEAR, 0.06f, 1.56f * mirror, -0.13f,
                            -41f * mirror, collapsed ? 78f : 59f, 16f * mirror, false),
                    new Pose(Prop.BANNER, -1.3f, -4.35f * mirror, 0f, 0f, 0f, 0f, false)
            );
            // Two oblique Talvars meet near the same point; the Dussak is fallen.
            case BLOOD_DUEL -> List.of(
                    new Pose(Prop.BLOOD_TALVAR, 0.10f, -0.47f * mirror, -0.11f,
                            31f * mirror, collapsed ? 42f : 18f, -34f * mirror, true),
                    new Pose(Prop.BLOOD_TALVAR, 0.18f, 0.43f * mirror, -0.12f,
                            -36f * mirror, 32f, 31f * mirror, true),
                    new Pose(Prop.BLOOD_DUSSAK, -0.73f, 1.08f * mirror, -0.19f,
                            68f * mirror, 82f, 14f * mirror, true),
                    new Pose(Prop.BLOOD_TALVAR, 1.06f, -1.54f * mirror, -0.21f,
                            -17f * mirror, 72f, -11f * mirror, false)
            );
            // Long heavy Spire blades make one impact knot; the knife is
            // physically subordinate and not treated as a fourth monument.
            case SPIRE_CLASH -> List.of(
                    new Pose(Prop.SPIRE_TORTURER_GREATSWORD, -0.18f, -0.5f * mirror, -0.18f,
                            25f * mirror, collapsed ? 38f : 17f, -21f * mirror, true),
                    new Pose(Prop.SPIRE_ROARING_AXE, 0.27f, 0.38f * mirror, -0.21f,
                            -40f * mirror, collapsed ? 67f : 39f, 30f * mirror, true),
                    new Pose(Prop.SPIRE_SHADOW_SCIMITAR, -0.84f, 1.11f * mirror, -0.22f,
                            65f * mirror, 76f, 7f * mirror, true),
                    new Pose(Prop.SPIRE_RAPID_KNIFE, 0.94f, -1.17f * mirror, -0.12f,
                            18f * mirror, 83f, -7f * mirror, false),
                    new Pose(Prop.BANNER, -1.42f, -4.35f * mirror, 0f, 0f, 0f, 0f, false)
            );
            // Source weapons match actual contracted-hunter loadouts, with
            // no assertion that real faction armies historically fought here.
            case HUNTER_TRACE -> List.of(
                    new Pose(Prop.SPIRE_SHADOW_SCIMITAR, -0.19f, -0.44f * mirror, -0.22f,
                            -26f * mirror, 41f, -29f * mirror, true),
                    new Pose(Prop.BLOOD_TALVAR, 0.21f, 0.37f * mirror, -0.11f,
                            31f * mirror, collapsed ? 70f : 34f, 20f * mirror, true),
                    new Pose(Prop.SPIRE_RAPID_KNIFE, 0.73f, 1.08f * mirror, -0.21f,
                            -64f * mirror, 80f, 10f * mirror, true),
                    new Pose(Prop.BLOOD_DUSSAK, -0.76f, -1.17f * mirror, -0.18f,
                            42f * mirror, 68f, -14f * mirror, false),
                    new Pose(Prop.CROSSBOW, 1.08f, -1.75f * mirror, -0.12f,
                            72f * mirror, 77f, -13f * mirror, false)
            );
            // Two approved standard swords at deliberately different heights.
            case PALE_OATH -> List.of(
                    new Pose(Prop.PALE_LINEAR_GREATSWORD, -0.20f, -0.52f * mirror, -0.26f,
                            16f * mirror, collapsed ? 37f : 14f, -17f * mirror, true),
                    new Pose(Prop.PALE_LONGSWORD, 0.36f, 0.35f * mirror, -0.20f,
                            -38f * mirror, collapsed ? 69f : 31f, 28f * mirror, true),
                    new Pose(Prop.SHIELD, -0.73f, 1.13f * mirror, -0.31f,
                            38f * mirror, 20f, -35f * mirror, true),
                    new Pose(Prop.SPEAR, 0.95f, -1.12f * mirror, -0.17f,
                            54f * mirror, 74f, 14f * mirror, false)
            );
            // Small blade-grove, intentionally curved around an OPEN middle,
            // not a grid; the large sword creates one distant silhouette.
            case BLADE_GROVE -> List.of(
                    new Pose(Prop.PALE_LINEAR_GREATSWORD, -0.53f, -3.31f * mirror, -0.35f,
                            14f * mirror, 12f, -11f * mirror, true),
                    new Pose(Prop.SPIRE_TORTURER_GREATSWORD, -0.9f, -1.78f * mirror, -0.37f,
                            -29f * mirror, collapsed ? 34f : 17f, 18f * mirror, true),
                    new Pose(Prop.PALE_LONGSWORD, 0.64f, 1.94f * mirror, -0.29f,
                            23f * mirror, 24f, -22f * mirror, true),
                    new Pose(Prop.SPIRE_ROARING_AXE, -0.67f, 3.37f * mirror, -0.24f,
                            -42f * mirror, collapsed ? 71f : 43f, 19f * mirror, true),
                    new Pose(Prop.BLOOD_DUSSAK, 0.81f, -2.93f * mirror, -0.20f,
                            67f * mirror, 81f, 10f * mirror, false),
                    new Pose(Prop.BLOOD_TALVAR, 0.36f, 3.64f * mirror, -0.23f,
                            -22f * mirror, 70f, -16f * mirror, false)
            );
            // A falling greatsword and broken defensive silhouette intersect.
            case FALLEN_EDGE -> List.of(
                    new Pose(Prop.SPIRE_TORTURER_GREATSWORD, -0.08f, 0.21f * mirror, -0.21f,
                            -27f * mirror, collapsed ? 79f : 61f, 31f * mirror, true),
                    new Pose(Prop.SHIELD, 0.33f, -0.42f * mirror, -0.37f,
                            29f * mirror, 24f, -34f * mirror, true),
                    new Pose(Prop.SPEAR, 0.71f, 0.87f * mirror, -0.14f,
                            58f * mirror, 78f, 17f * mirror, false),
                    new Pose(Prop.SPIRE_RAPID_KNIFE, -0.83f, -0.92f * mirror, -0.18f,
                            -41f * mirror, 75f, 14f * mirror, false)
            );
            // A zigzag between two banners with fallen mixed armaments, not
            // another identical shield+spear knot.
            case TWIN_STANDARDS -> List.of(
                    new Pose(Prop.BANNER, -1.35f, -4.5f * mirror, 0f, 0f, 0f, 0f, true),
                    new Pose(Prop.BANNER, 1.20f, 4.7f * mirror, 0f, 0f, 0f, 0f, true),
                    new Pose(Prop.PALE_LONGSWORD, -0.10f, -0.49f * mirror, -0.23f,
                            18f * mirror, collapsed ? 68f : 35f, -24f * mirror, true),
                    new Pose(Prop.SPIRE_ROARING_AXE, 0.48f, 0.48f * mirror, -0.27f,
                            -37f * mirror, 65f, 26f * mirror, true),
                    new Pose(Prop.SHIELD, -0.59f, 1.23f * mirror, -0.29f,
                            42f * mirror, 19f, 28f * mirror, false)
            );
            // Crossbow directions form intersecting trails; no fake bolts or
            // false claim that a real projectile model exists.
            case CROSS_FIRE -> List.of(
                    new Pose(Prop.CROSSBOW, -0.62f, -0.85f * mirror, -0.11f,
                            35f * mirror, 70f, -14f * mirror, true),
                    new Pose(Prop.CROSSBOW, 0.35f, 0.78f * mirror, -0.12f,
                            -41f * mirror, collapsed ? 83f : 64f, 11f * mirror, true),
                    new Pose(Prop.SHIELD, -0.06f, 0.03f, -0.36f,
                            12f * mirror, 24f, -28f * mirror, true),
                    new Pose(Prop.PALE_LONGSWORD, 1.02f, -1.67f * mirror, -0.18f,
                            55f * mirror, 80f, -10f * mirror, false),
                    new Pose(Prop.SPIRE_RAPID_KNIFE, -1.12f, 1.67f * mirror, -0.18f,
                            73f * mirror, 75f, -9f * mirror, false)
            );
            // Quiet outer edge: gaps are intentional between the three
            // surviving weapons; not another battlefield-center object pile.
            case FINAL_TRACE -> List.of(
                    new Pose(Prop.PALE_LONGSWORD, -0.91f, -2.6f * mirror, -0.29f,
                            25f * mirror, collapsed ? 47f : 14f, -18f * mirror, true),
                    new Pose(Prop.SPIRE_SHADOW_SCIMITAR, 0.55f, 0.05f, -0.22f,
                            -54f * mirror, 76f, 18f * mirror, true),
                    new Pose(Prop.BLOOD_DUSSAK, -0.74f, 2.59f * mirror, -0.26f,
                            34f * mirror, 85f, -21f * mirror, true),
                    new Pose(Prop.CROSSBOW, 0.91f, 3.45f * mirror, -0.12f,
                            65f * mirror, 77f, -7f * mirror, false)
            );
            default -> throw new IllegalArgumentException("No legacy scene: " + scene);
        };
    }
}
