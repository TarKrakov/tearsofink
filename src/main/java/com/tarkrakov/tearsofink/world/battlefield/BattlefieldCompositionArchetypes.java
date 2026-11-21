package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

import java.util.List;
import java.util.ArrayList;

/**
 * D-127: different battlefield EVENTS, not a 2x2 scatter of upright items.
 *
 * Local axes: forward follows the episode, right crosses it, both in blocks.
 * Relations live in the topology: ruptures, arcs, opposite approaches,
 * narrow gaps, abandoned trails, isolated survivors and weapon banks.
 *
 * Every motif has a required minimum silhouette and optional evidence.
 * Positions are intentionally authored in story-space, with only small
 * deterministic variation. No new textures, lore, entities or worldgen.
 * This catalog is the CURRENT (layout=1) manual visual-preview grammar.
 * D-125 (layout=0) remains preserved separately for existing saved scenes.
 */
public final class BattlefieldCompositionArchetypes {
    private BattlefieldCompositionArchetypes() {}

    // An explicit silhouette classification, for coverage tests/documentation,
    // NOT a procedural random mode. One class of arrangement per scenario.
    public enum Silhouette {
        SINGLE_KNOT, BROKEN_LINE, BREACHED_ARC, COLLAPSING_FAN,
        RETREAT_TRAIL, DUEL, TWO_SIDED_IMPACT, ZIGZAG_TRAIL,
        ASYMMETRIC_PAIR, TWO_BANKS, FALLEN_SLASH, OPEN_GATE,
        OPPOSED_FIRE, DISTANT_TRACES, SHIELD_CRESCENT,
        SMALL_WEAPON_SCATTER, HEAVY_BREACH, BLADE_ROSETTE,
        ABANDONED_CACHE, LONELY_LANDMARK
    }

    public static Silhouette silhouette(Scenario scene) {
        return Silhouette.values()[scene.ordinal()];
    }

    private static Pose at(Prop prop, float forward, float right, float sink,
                           float yaw, float pitch, float roll, boolean required) {
        return new Pose(prop, forward, right, sink, yaw, pitch, roll, required);
    }

    public static List<Pose> plan(Scenario scenario, int variant) {
        int m = (variant & 1) == 0 ? 1 : -1;
        boolean alternate = (variant & 4) != 0;
        float shift = (variant & 2) == 0 ? -0.26f : 0.26f;
        // A different blade can emphasize the same scene's story without
        // changing which end points to the ground (see D-126).
        Prop strayBlade = alternate ? Prop.BLOOD_TALVAR : Prop.PALE_LONGSWORD;
        Prop heavyBlade = alternate ? Prop.PALE_LINEAR_GREATSWORD
                                    : Prop.SPIRE_TORTURER_GREATSWORD;
        List<Pose> composed = switch (scenario) {
            // One readable focal point; a second ruined weapon is a trace,
            // not another symmetric post.
            case LAST_STAND -> List.of(
                    at(Prop.SHIELD, 0.0f, 0f, -0.29f, 16f*m, -8f, -23f*m, true),
                    at(Prop.SPEAR, 0.46f, (-0.52f+shift)*m, -0.11f,
                            -12f*m, 25f, -30f*m, true),
                    at(Prop.CROSSBOW, -0.65f, 1.68f*m, -0.12f,
                            71f*m, 78f, -9f*m, false),
                    at(strayBlade, 1.68f, 1.20f*m, -0.14f,
                            -52f*m, alternate ? 73f : 42f, 16f*m, false)
            );
            // Two high banners, but the fighting occurs at the LEFT foot:
            // the middle between banners is intentionally open.
            case BANNER_LINE -> List.of(
                    at(Prop.BANNER, -1.75f, -5.15f*m, 0, 0, 0, 0, true),
                    at(Prop.BANNER, 1.95f, 5.10f*m, 0, 0, 0, 0, true),
                    at(Prop.SHIELD, -0.7f, -2.60f*m, -0.37f,
                            29f*m, 18f, 24f*m, true),
                    at(Prop.SPEAR, -0.28f, (-1.78f+shift)*m, -0.10f,
                            -34f*m, 47f, 32f*m, true),
                    at(strayBlade, 0.72f, 3.26f*m, -0.12f,
                            49f*m, 76f, -9f*m, false)
            );
            // A three-shield ARC with a deliberate breach to the right of
            // centre, one blade aimed into the breach rather than random dust.
            case BROKEN_WALL -> List.of(
                    at(Prop.SHIELD, -1.88f, -4.04f*m, -0.31f, -12f*m, 10f, -18f*m, true),
                    at(Prop.SHIELD, -2.45f, -2.03f*m, -0.38f, 15f*m, 26f, -37f*m, true),
                    at(Prop.SHIELD, -1.71f, 3.86f*m, -0.22f, 36f*m, 20f, 29f*m, true),
                    at(Prop.SPEAR, -0.57f, -3.15f*m, -0.12f,
                            60f*m, 51f, 8f*m, false),
                    at(heavyBlade, 0.42f, 0.55f*m, -0.23f,
                            -22f*m, alternate ? 63f : 26f, 16f*m, true),
                    at(Prop.SPEAR, 1.60f, 2.65f*m, -0.12f, -43f*m, 78f, 12f*m, false)
            );
            // Weapons fall as a FAN in the direction of one lost advance:
            // multiple spear shafts converge toward the last shield.
            case SPEAR_COLLAPSE -> List.of(
                    at(Prop.SPEAR, -2.42f, -3.38f*m, -0.10f, 56f*m, 43f, -22f*m, true),
                    at(Prop.SPEAR, -0.78f, -1.64f*m, -0.13f, 20f*m, 74f, 14f*m, true),
                    at(Prop.SPEAR, 0.68f, 0.51f*m, -0.08f, -40f*m, alternate ? 71f : 52f, 23f*m, true),
                    at(Prop.SPEAR, 1.91f, 2.92f*m, -0.16f, -62f*m, 80f, -12f*m, false),
                    at(Prop.SHIELD, 2.01f, -0.62f*m, -0.34f, -19f*m, 34f, 32f*m, true)
            );
            // A long diagonal TRAIL: each abandoned object is a different
            // moment of retreat. No central knot or balanced weapon bouquet.
            case ABANDONED_POST -> List.of(
                    at(Prop.SHIELD, -3.37f, -4.36f*m, -0.34f, 21f*m, 21f, -29f*m, true),
                    at(Prop.CROSSBOW, -1.48f, -1.68f*m, -0.10f, -54f*m, 69f, 13f*m, true),
                    at(Prop.BLOOD_DUSSAK, 0.82f, 1.22f*m, -0.13f, 51f*m, 84f, -18f*m, true),
                    at(Prop.SPEAR, 3.23f, 4.18f*m, -0.11f, 33f*m, 77f, 17f*m, false),
                    at(Prop.SPIRE_RAPID_KNIFE, 2.35f, -1.01f*m, -0.08f, -42f*m, 79f, -9f*m, false)
            );
            // OPPOSING curved blades converge toward an open center. The third
            // weapon has already fallen OUTSIDE the line of contact.
            case BLOOD_DUEL -> List.of(
                    at(Prop.BLOOD_TALVAR, -0.72f, -2.33f*m, -0.11f, -31f*m, 20f, 37f*m, true),
                    at(Prop.BLOOD_TALVAR, 0.78f, 2.31f*m, -0.14f, 37f*m, 34f, -34f*m, true),
                    at(Prop.BLOOD_DUSSAK, 1.70f, -0.21f*m, -0.12f, -64f*m, 79f, -16f*m, true),
                    at(Prop.BLOOD_TALVAR, -2.00f, 3.97f*m, -0.12f, 48f*m, 82f, 10f*m, false)
            );
            // Huge weapons face one another across an empty IMPACT point;
            // a knife at the edge gives the scale contrast.
            case SPIRE_CLASH -> List.of(
                    at(Prop.SPIRE_TORTURER_GREATSWORD, -1.42f, -2.42f*m, -0.18f,
                            23f*m, alternate ? 41f : 20f, -27f*m, true),
                    at(Prop.SPIRE_ROARING_AXE, 1.62f, 2.73f*m, -0.20f,
                            -46f*m, 38f, 27f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, 2.04f, -0.44f*m, -0.12f,
                            -53f*m, 79f, 21f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, -2.42f, 3.23f*m, -0.08f, 69f*m, 83f, -7f*m, false)
            );
            // A broken ZIGZAG of hunter weapons, with two distinct bends,
            // instead of four handles emerging from the same square.
            case HUNTER_TRACE -> List.of(
                    at(Prop.SPIRE_RAPID_KNIFE, -3.57f, -4.14f*m, -0.08f, 58f*m, 73f, -18f*m, true),
                    at(Prop.BLOOD_TALVAR, -1.34f, -1.65f*m, -0.14f, -24f*m, 33f, 29f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, 0.88f, 1.34f*m, -0.13f, 34f*m, 77f, -13f*m, true),
                    at(Prop.BLOOD_DUSSAK, 3.12f, 4.05f*m, -0.13f, -56f*m, 75f, -9f*m, false),
                    at(Prop.CROSSBOW, 2.42f, -3.05f*m, -0.11f, -71f*m, 73f, 10f*m, false)
            );
            // Two Pale weapons deliberately DO NOT touch: one embedded great
            // blade, one fallen sword across an open patch of ground.
            case PALE_OATH -> List.of(
                    at(Prop.PALE_LINEAR_GREATSWORD, -1.02f, -2.40f*m, -0.28f, 13f*m, 15f, -16f*m, true),
                    at(Prop.PALE_LONGSWORD, 1.72f, 2.14f*m, -0.09f, -54f*m, 82f, 15f*m, true),
                    at(Prop.SHIELD, -2.30f, -3.63f*m, -0.31f, 22f*m, 28f, -27f*m, false),
                    at(Prop.SPEAR, 3.19f, 3.85f*m, -0.11f, 74f*m, 66f, 8f*m, false)
            );
            // TWO BANKS of planted swords. The central corridor remains
            // completely empty, with irregular gaps along either bank.
            case BLADE_GROVE -> List.of(
                    at(Prop.PALE_LINEAR_GREATSWORD, -3.21f, -3.17f*m, -0.31f, 12f*m, 16f, -9f*m, true),
                    at(Prop.SPIRE_TORTURER_GREATSWORD, -0.92f, -4.28f*m, -0.30f, -24f*m, 22f, 19f*m, true),
                    at(Prop.BLOOD_TALVAR, 1.74f, -3.31f*m, -0.16f, 42f*m, 71f, -10f*m, false),
                    at(Prop.SPIRE_ROARING_AXE, 3.35f, -4.74f*m, -0.20f, 57f*m, 50f, 15f*m, false),
                    at(Prop.PALE_LONGSWORD, -2.88f, 3.38f*m, -0.24f, -21f*m, 20f, 14f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, -0.42f, 4.73f*m, -0.12f, 28f*m, 35f, -21f*m, true),
                    at(Prop.BLOOD_DUSSAK, 2.42f, 3.27f*m, -0.09f, -64f*m, 78f, 12f*m, false)
            );
            // One heavy blade cuts diagonally across a tipped shield.
            // A second, small weapon is far off that slash line.
            case FALLEN_EDGE -> List.of(
                    at(Prop.SPIRE_TORTURER_GREATSWORD, -1.45f, -0.20f*m, -0.09f,
                            -35f*m, alternate ? 81f : 68f, 23f*m, true),
                    at(Prop.SHIELD, 0.78f, -1.94f*m, -0.37f, 41f*m, 27f, -39f*m, true),
                    at(Prop.SPEAR, 1.96f, 2.33f*m, -0.10f, -56f*m, 80f, 6f*m, false),
                    at(Prop.SPIRE_RAPID_KNIFE, -2.67f, 3.50f*m, -0.08f, 66f*m, 75f, -18f*m, false)
            );
            // An OPEN GATE between two banners. Different weapon remnants on
            // opposite flanks; the center stays available as a path.
            case TWIN_STANDARDS -> List.of(
                    at(Prop.BANNER, -2.30f, -5.29f*m, 0f, 0f, 0f, 0f, true),
                    at(Prop.BANNER, 2.25f, 5.25f*m, 0f, 0f, 0f, 0f, true),
                    at(Prop.SPIRE_ROARING_AXE, -0.82f, -3.56f*m, -0.15f, 49f*m, 43f, -19f*m, true),
                    at(Prop.PALE_LONGSWORD, 0.94f, 3.48f*m, -0.15f, -38f*m, 73f, 13f*m, true),
                    at(Prop.SHIELD, 2.02f, 2.39f*m, -0.31f, 19f*m, 25f, -23f*m, false)
            );
            // Two opposing ARBALISTS on distant flanks, shield between.
            // NO fake projectiles or new artwork.
            case CROSS_FIRE -> List.of(
                    at(Prop.CROSSBOW, -2.75f, -4.02f*m, -0.10f, 57f*m, 67f, 11f*m, true),
                    at(Prop.CROSSBOW, 2.81f, 4.12f*m, -0.11f, -54f*m, 72f, -9f*m, true),
                    at(Prop.SHIELD, -0.02f, 0.11f, -0.40f, 7f*m, 27f, -26f*m, true),
                    at(Prop.CROSSBOW, 2.05f, -3.43f*m, -0.10f, -71f*m, 79f, -8f*m, false),
                    at(Prop.SPIRE_RAPID_KNIFE, -2.56f, 2.63f*m, -0.08f, 70f*m, 76f, 13f*m, false)
            );
            // A sparse set of DISTANT TRACES, not a pile: three objects span
            // nearly ten blocks with no required central decoration.
            case FINAL_TRACE -> List.of(
                    at(Prop.PALE_LONGSWORD, -3.47f, -4.38f*m, -0.24f, 23f*m, 21f, -12f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, 0.12f, -0.52f*m, -0.11f, -46f*m, 79f, 11f*m, true),
                    at(Prop.BLOOD_DUSSAK, 3.23f, 4.13f*m, -0.12f, 61f*m, 78f, -15f*m, true)
            );
            // Curved defense but an OPEN INTERIOR. No straight line of poles.
            case SHIELD_CRESCENT -> List.of(
                    at(Prop.SHIELD, -2.20f, -3.76f*m, -0.29f, -30f*m, 10f, -12f*m, true),
                    at(Prop.SHIELD, -2.86f, -1.45f*m, -0.32f, -9f*m, 25f, -31f*m, true),
                    at(Prop.SHIELD, -2.93f, 1.71f*m, -0.29f, 18f*m, 12f, 27f*m, true),
                    at(Prop.SHIELD, -1.92f, 3.82f*m, -0.35f, 36f*m, 30f, 17f*m, false),
                    at(Prop.SPEAR, -0.88f, -3.18f*m, -0.12f, 49f*m, 44f, -10f*m, false),
                    at(Prop.SPEAR, -0.78f, 3.25f*m, -0.13f, -42f*m, 78f, 17f*m, false)
            );
            // Lots of SMALL WEAPONS in several directions, rather than the
            // large sword monuments that dominate the other zones.
            case KNIFE_SCATTER -> List.of(
                    at(Prop.SPIRE_RAPID_KNIFE, -2.24f, -2.94f*m, -0.09f, 36f*m, 80f, 8f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, -0.56f, 0.17f*m, -0.08f, -71f*m, 85f, -12f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, 2.02f, 3.07f*m, -0.09f, 67f*m, 82f, 10f*m, true),
                    at(Prop.BLOOD_DUSSAK, 1.64f, -1.90f*m, -0.12f, -28f*m, 75f, -17f*m, false),
                    at(alternate ? Prop.BLOOD_TALVAR : Prop.SPIRE_RAPID_KNIFE,
                            -3.25f, 3.63f*m, -0.08f, 16f*m, 77f, 11f*m, false)
            );
            // Two AXES framing a clean breach, with a shield shoved out
            // of its original defensive position.
            case AXE_BREACH -> List.of(
                    at(Prop.SPIRE_ROARING_AXE, -1.43f, -3.21f*m, -0.18f, 28f*m, 24f, -23f*m, true),
                    at(Prop.SPIRE_ROARING_AXE, 1.41f, 3.38f*m, -0.19f, -42f*m, 48f, 19f*m, true),
                    at(Prop.SHIELD, -2.75f, -4.33f*m, -0.37f, -7f*m, 36f, -27f*m, true),
                    at(heavyBlade, 2.22f, -0.12f*m, -0.14f, 48f*m, 77f, -10f*m, false)
            );
            // A ROSETTE: the handles point outward from a vacant centre.
            // Not the two long straight banks of BLADE_GROVE.
            case BLADE_FAN -> List.of(
                    at(Prop.PALE_LINEAR_GREATSWORD, -2.41f, -1.44f*m, -0.22f, -49f*m, 42f, -17f*m, true),
                    at(Prop.PALE_LONGSWORD, -0.96f, -3.34f*m, -0.19f, 22f*m, 28f, 18f*m, true),
                    at(Prop.SPIRE_SHADOW_SCIMITAR, 1.72f, -2.72f*m, -0.14f, 57f*m, 74f, 12f*m, true),
                    at(Prop.BLOOD_TALVAR, 2.44f, 1.10f*m, -0.14f, -62f*m, 30f, -18f*m, true),
                    at(Prop.SPIRE_TORTURER_GREATSWORD, -0.91f, 3.25f*m, -0.22f, 36f*m, 25f, -23f*m, false),
                    at(Prop.BLOOD_DUSSAK, -2.47f, 1.65f*m, -0.12f, -31f*m, 82f, 9f*m, false)
            );
            // Dense, asymmetric CACHE of crossbows with a shield kept FAR
            // outside the cluster; the empty gap between them matters.
            case CROSSBOW_CACHE -> List.of(
                    at(Prop.CROSSBOW, -1.87f, -2.28f*m, -0.10f, 38f*m, 74f, -9f*m, true),
                    at(Prop.CROSSBOW, -2.53f, -0.93f*m, -0.10f, -24f*m, 80f, 14f*m, true),
                    at(Prop.CROSSBOW, -0.92f, -3.73f*m, -0.10f, 75f*m, 68f, -12f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, -1.53f, -3.05f*m, -0.10f, 61f*m, 84f, -11f*m, false),
                    at(Prop.SHIELD, 2.76f, 3.56f*m, -0.31f, 28f*m, 29f, -27f*m, false)
            );
            // A single huge embedded blade is the composition. The second
            // mandatory tiny, distant trace provides scale without a "pile".
            case LONE_BLADE -> List.of(
                    at(heavyBlade, -0.12f, -0.08f*m, -0.32f,
                            12f*m, alternate ? 35f : 10f, -11f*m, true),
                    at(Prop.SPIRE_RAPID_KNIFE, 3.38f, 4.36f*m, -0.07f,
                            74f*m, 81f, -8f*m, true),
                    at(Prop.CROSSBOW, -3.05f, -4.26f*m, -0.10f, -42f*m, 84f, 8f*m, false)
            );
            default -> throw new IllegalArgumentException("D-131 scenes use BattlefieldHistoricalScenes: " + scenario);
        };
        if (!alternate) return composed;

        // The second pose family changes the silhouette of a peripheral
        // remnant (not the central story-bearing contact). This is a real
        // displacement / different fall direction, not just another yaw.
        // Kept bounded so the terrain planner can still search naturally.
        int last = composed.size() - 1;
        while (last >= 0 && composed.get(last).banner()) last--;
        if (last < 0) return composed;
        Pose p = composed.get(last);
        float df = Math.abs(p.forward()) >= 2.9f
                ? -Math.signum(p.forward()) * 0.34f
                : (p.forward() >= 0f ? 0.46f : -0.46f);
        float dr = Math.abs(p.right()) >= 4.9f
                ? -Math.signum(p.right()) * 0.30f
                : (p.right() >= 0f ? 0.38f : -0.38f);
        List<Pose> changed = new ArrayList<>(composed);
        changed.set(last, at(p.prop(), p.forward() + df, p.right() + dr,
                p.sink(), p.yaw() + (p.yaw() > 0f ? -11f : 11f),
                p.pitch(), p.roll(), p.required()));
        return List.copyOf(changed);
    }
}
