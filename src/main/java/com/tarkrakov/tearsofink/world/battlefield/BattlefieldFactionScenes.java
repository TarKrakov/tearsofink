package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import java.util.ArrayList;
import java.util.List;

/**
 * D-133: real authored faction flags attached to historical moments, without
 * rewriting a SINGLE saved D-131 layout=2 scene. Only layout=3 opts in.
 * Flags express a line, rally, loss or broken defence, not a quota per scene.
 */
public final class BattlefieldFactionScenes {
    private BattlefieldFactionScenes() {}
    public static List<Pose> withFactionBanners(Scenario scene, int variant, List<Pose> base) {
        if (scene.ordinal() < 20) return base;
        int mirror = (variant & 1) == 0 ? 1 : -1;
        float slip = (variant & 2) == 0 ? -.18f : .18f;
        Pose banner = switch (scene) {
            // Both ancient defender factions get their OWN full and bare rigs.
            case PALADIN_HOLDOUT_ANCHOR ->
                new Pose(Prop.PALADIN_BANNER, -2.55f, (-5.05f+slip)*mirror, 0, 0, 0, 0, true);
            case PALADIN_BARE_RELIC ->
                new Pose(Prop.PALADIN_BANNER_BARE, 2.11f, (4.98f+slip)*mirror, 0, 0, 0, 0, true);
            case HUNTER_STRIKE_MARK ->
                new Pose(Prop.HUNTER_BANNER, -2.60f, (5.00f+slip)*mirror, 0, 0, 0, 0, true);
            case HUNTER_BARE_TRACE ->
                new Pose(Prop.HUNTER_BANNER_BARE, -2.64f, (-5.15f+slip)*mirror, 0, 0, 0, 0, true);
            // Additional paladin compositions: their shields remain the core.
            // Banners are optional where a narrow slope might prevent a flag.
            case PALADIN_SOUL_SHIELD_WALL ->
                new Pose(Prop.PALADIN_BANNER, -2.84f, (-5.10f+slip)*mirror, 0, 0, 0, 0, false);
            case PALADIN_MERCY_HOLDOUT ->
                new Pose(Prop.PALADIN_BANNER, -2.72f, (-4.88f+slip)*mirror, 0, 0, 0, 0, false);
            case PALADIN_BROKEN_HALO ->
                new Pose(Prop.PALADIN_BANNER_BARE, -2.83f, (5.14f+slip)*mirror, 0, 0, 0, 0, false);
            // Ink Hunters: a fighting front / scattered trace / lost pole.
            case INK_HUNTER_SWEEP ->
                new Pose(Prop.HUNTER_BANNER, -2.73f, (-5.03f+slip)*mirror, 0, 0, 0, 0, false);
            case INK_HUNTER_AMBUSH ->
                new Pose(Prop.HUNTER_BANNER_BARE, -2.85f, (5.11f+slip)*mirror, 0, 0, 0, 0, false);
            // Lone last trace deliberately has no banner; silence is meaningful.
            default -> null;
        };
        if (banner == null) return base;
        List<Pose> result = new ArrayList<>(base);
        result.add(banner); // Never reorder old poses/Contacts.
        return List.copyOf(result);
    }
}
