package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

import java.util.ArrayList;
import java.util.List;

/**
 * D-131: fourteen NEW situation-led or real-banner-led, spatially authored
 * manual battlefield vignettes. 8 deterministic variants each.
 *
 * Local forward/right coordinates are in blocks; independent yaw/pitch/roll
 * are pivoted by the existing D-126 tip-down renderer and terrain sampler.
 * No worldgen. No guessed paladin or Ink Hunter banners: those scenes use
 * only existing author-approved props while their faction art is pending.
 * Historical affiliation of current item meshes is NOT canonized by previews.
 */
public final class BattlefieldHistoricalScenes {
    private BattlefieldHistoricalScenes() {}

    private static Pose at(Prop prop, float forward, float right, float sink,
                           float yaw, float pitch, float roll, boolean required) {
        return new Pose(prop, forward, right, sink, yaw, pitch, roll, required);
    }

    public static List<Pose> plan(Scenario scene, int variant) {
        int m = (variant & 1) == 0 ? 1 : -1;
        float shift = (variant & 2) == 0 ? -0.22f : 0.22f;
        boolean other = (variant & 4) != 0;
        // These are author's EXISTING item meshes, not claims about what
        // weapons ancient defenders canonically carried.
        Prop heavy = other ? Prop.PALE_LINEAR_GREATSWORD : Prop.SPIRE_TORTURER_GREATSWORD;
        Prop shortBlade = other ? Prop.BLOOD_DUSSAK : Prop.PALE_LONGSWORD;

        List<Pose> base = switch (scene) {
            // A defended REGULAR ARMY squad standard. The banner anchors the
            // left rear, while the linked defense forms a forward-facing arc.
            case REGULAR_SQUAD_LINE_ANCHOR -> List.of(
                    at(Prop.BANNER, -2.0f, -4.35f*m, 0, 0, 0, 0, true),
                    at(Prop.SHIELD, -0.75f, -1.6f*m, -0.32f, 12f*m, 12, -24f*m, true),
                    at(Prop.SPEAR, 0.1f, -0.58f*m, -0.11f, -23f*m, 42, 22f*m, true),
                    at(Prop.SHIELD, -0.35f, 2.0f*m, -0.35f, -19f*m, 29, 24f*m, true),
                    at(Prop.CROSSBOW, 1.82f, 2.73f*m, -0.11f, 66f*m, 78, -12f*m, false)
            );
            // Rally banner slightly behind the line, NOT an altar in a heap.
            case REGULAR_SQUAD_RALLY_POINT -> List.of(
                    at(Prop.BANNER, -2.50f, -3.90f*m, 0, 0, 0, 0, true),
                    at(Prop.SHIELD, -0.92f, 0.16f*m, -0.31f, 39f*m, 23, -25f*m, true),
                    at(Prop.CROSSBOW, 0.91f, 1.54f*m, -0.10f, -38f*m, 75, 14f*m, true),
                    at(Prop.SPEAR, 1.81f, -0.65f*m, -0.11f, -56f*m, 73, -14f*m, false),
                    at(Prop.PALE_LONGSWORD, 2.55f, 3.33f*m, -0.11f, 68f*m, 79, 10f*m, false)
            );
            // The actual authored flagless standard and a BROKEN line.
            case REGULAR_BARE_REMNANT -> List.of(
                    at(Prop.BANNER_BARE, -2.30f, -4.46f*m, 0, 0, 0, 0, true),
                    at(Prop.SHIELD, -0.94f, -0.82f*m, -0.39f, 19f*m, 38, -35f*m, true),
                    at(Prop.SPEAR, 0.57f, 1.14f*m, -0.11f, -57f*m, 80, 18f*m, true),
                    at(Prop.CROSSBOW, 2.16f, 3.27f*m, -0.10f, 39f*m, 82, -11f*m, false)
            );
            // PALADIN BANNER ART NOT PRESENT. This is a bannerless provisional
            // resistance knot, deliberately no regular-army stand-in.
            case PALADIN_HOLDOUT_ANCHOR -> List.of(
                    at(Prop.SHIELD, -1.38f, -2.02f*m, -0.31f, -16f*m, 9, -14f*m, true),
                    at(Prop.PALE_LINEAR_GREATSWORD, -0.73f, 0.26f*m, -0.26f, 15f*m, 19, 15f*m, true),
                    at(Prop.SHIELD, -1.15f, 2.34f*m, -0.36f, 23f*m, 27, 29f*m, true),
                    at(Prop.PALE_LONGSWORD, 1.44f, -3.29f*m, -0.11f, -62f*m, 79, -13f*m, false)
            );
            // Quiet remnant; a paladin bare standard is NOT fabricated.
            case PALADIN_BARE_RELIC -> List.of(
                    at(Prop.PALE_LINEAR_GREATSWORD, -0.65f, -1.53f*m, -0.30f, 22f*m, 15, -12f*m, true),
                    at(Prop.SHIELD, 0.40f, 1.03f*m, -0.42f, -35f*m, 41, 32f*m, true),
                    at(Prop.SPEAR, 1.70f, 3.44f*m, -0.11f, 75f*m, 81, -12f*m, false)
            );
            // HUNTER BANNER ART NOT PRESENT. Only currently sourced blades,
            // grouped as a fierce directional trace, no invented lore assets.
            case HUNTER_STRIKE_MARK -> List.of(
                    at(Prop.SPIRE_SHADOW_SCIMITAR, -2.90f, -3.52f*m, -0.13f, 28f*m, 26, 18f*m, true),
                    at(Prop.BLOOD_TALVAR, -0.57f, -1.58f*m, -0.15f, -42f*m, 32, -24f*m, true),
                    at(Prop.SPIRE_ROARING_AXE, 1.78f, 1.61f*m, -0.18f, 56f*m, 52, 25f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, 2.57f, 3.43f*m, -0.09f, -70f*m, 81, 12f*m, false)
            );
            // No hunter bare standard until real model is uploaded.
            case HUNTER_BARE_TRACE -> List.of(
                    at(Prop.SPIRE_RAPID_KNIFE, -2.12f, -4.15f*m, -0.09f, 40f*m, 85, -13f*m, true),
                    at(Prop.BLOOD_DUSSAK, -0.22f, -1.33f*m, -0.12f, -24f*m, 78, 11f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, 2.01f, 2.90f*m, -0.11f, 56f*m, 74, -17f*m, true)
            );
            // No banner: a line bows inward and opens a distinct hole.
            case LINE_COLLAPSE -> List.of(
                    at(Prop.SHIELD, -2.30f, -3.71f*m, -0.33f, -21f*m, 17, -28f*m, true),
                    at(Prop.SHIELD, -2.75f, -0.77f*m, -0.42f, 26f*m, 41, 35f*m, true),
                    at(Prop.SHIELD, -1.95f, 3.46f*m, -0.30f, 36f*m, 22, -21f*m, true),
                    at(Prop.SPEAR, 0.84f, 0.38f*m, -0.12f, 58f*m, 79, -14f*m, false),
                    at(Prop.PALE_LONGSWORD, 1.62f, 2.24f*m, -0.12f, -38f*m, 78, 17f*m, false)
            );
            // Density concentrated at the contact, with quiet outer edges.
            case GRIND_CENTER -> List.of(
                    at(Prop.SPIRE_ROARING_AXE, -0.70f, -1.48f*m, -0.21f, 28f*m, 32, -23f*m, true),
                    at(Prop.PALE_LINEAR_GREATSWORD, 0.28f, 0.87f*m, -0.25f, -31f*m, 24, 22f*m, true),
                    at(Prop.SHIELD, -1.44f, 2.41f*m, -0.32f, 38f*m, 29, -29f*m, true),
                    at(Prop.SPEAR, 1.32f, -2.26f*m, -0.12f, -63f*m, 70, 17f*m, false),
                    at(Prop.BLOOD_TALVAR, 2.41f, 3.44f*m, -0.11f, 54f*m, 81, -14f*m, false)
            );
            // Two broken banks create a traversable breakthrough corridor.
            case BREAKTHROUGH_LANE -> List.of(
                    at(Prop.SHIELD, -2.89f, -3.73f*m, -0.38f, 18f*m, 34, -25f*m, true),
                    at(Prop.SPEAR, -0.85f, -3.49f*m, -0.12f, -38f*m, 67, 15f*m, true),
                    at(heavy, 1.55f, -3.18f*m, -0.23f, 44f*m, 33, -21f*m, false),
                    at(Prop.SHIELD, -1.92f, 3.51f*m, -0.37f, -26f*m, 40, 28f*m, true),
                    at(Prop.PALE_LONGSWORD, 0.92f, 3.74f*m, -0.12f, 51f*m, 78, -12f*m, true)
            );
            // Small dispersed remnants, no repeated visual landmark.
            case AFTERMATH_SCATTER -> List.of(
                    at(Prop.PALE_LONGSWORD, -2.91f, -3.63f*m, -0.10f, 46f*m, 80, -9f*m, true),
                    at(Prop.CROSSBOW, 0.17f, 0.67f*m, -0.09f, -54f*m, 77, 10f*m, true),
                    at(Prop.BLOOD_DUSSAK, 2.44f, 3.78f*m, -0.11f, 27f*m, 83, -12f*m, true)
            );
            // Diagonal withdrawal from a broken guard at left to dropped kit.
            case REAR_GUARD_FALLBACK -> List.of(
                    at(Prop.SHIELD, -3.12f, -3.76f*m, -0.32f, -27f*m, 17, 22f*m, true),
                    at(Prop.SPEAR, -1.14f, -1.46f*m, -0.11f, 34f*m, 70, -12f*m, true),
                    at(Prop.CROSSBOW, 1.14f, 1.02f*m, -0.10f, -57f*m, 78, 14f*m, true),
                    at(shortBlade, 3.24f, 3.71f*m, -0.12f, 63f*m, 80, -8f*m, false)
            );
            // A tiny defensive knot, NO standard to cheapen its solitude.
            case ISOLATED_LAST_STAND_SMALL -> List.of(
                    at(Prop.SHIELD, -0.74f, -0.84f*m, -0.35f, 13f*m, 32, -28f*m, true),
                    at(Prop.SPEAR, 0.22f, -0.19f*m, -0.11f, -22f*m, 27, 23f*m, true),
                    at(Prop.PALE_LONGSWORD, 1.56f, 2.74f*m, -0.11f, 57f*m, 81, -10f*m, false)
            );
            // Quiet, mixed ground after the battle. Distinct empty center.
            case MIXED_DEAD_GROUND -> List.of(
                    at(Prop.CROSSBOW, -2.69f, -4.13f*m, -0.10f, 65f*m, 80, -10f*m, true),
                    at(Prop.BLOOD_DUSSAK, 0.19f, -1.05f*m, -0.12f, -35f*m, 78, 13f*m, true),
                    at(Prop.PALE_LONGSWORD, 2.75f, 3.93f*m, -0.12f, 52f*m, 82, -13f*m, true),
                    at(Prop.SPEAR, -2.02f, 3.48f*m, -0.10f, -62f*m, 76, 11f*m, false)
            );
            default -> throw new IllegalArgumentException("Not a D-131 historical preset: " + scene);
        };

        if (!other) return base;
        // Variant 4+ changes a peripheral prop's actual position and fall,
        // leaving the focal banner and required spatial story intact.
        int index = base.size() - 1;
        while (index >= 0 && base.get(index).banner()) index--;
        if (index < 0) return base;
        Pose p = base.get(index);
        float df = Math.abs(p.forward()) > 2.8f ? -Math.signum(p.forward()) * .35f : .48f;
        float dr = Math.abs(p.right()) > 4.7f ? -Math.signum(p.right()) * .25f : .44f;
        List<Pose> changed = new ArrayList<>(base);
        changed.set(index, at(p.prop(), p.forward() + df, p.right() + dr,
                p.sink(), p.yaw() + (p.yaw() < 0 ? 12f : -12f),
                p.pitch(), p.roll(), p.required()));
        return List.copyOf(changed);
    }
}
