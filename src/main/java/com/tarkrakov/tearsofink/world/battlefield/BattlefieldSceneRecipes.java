package com.tarkrakov.tearsofink.world.battlefield;

import java.util.List;

/**
 * Two deliberately small *technical preview* recipes, not canonical battlefield
 * content or world generation. Every object has a relationship to the same
 * local zone axis, rather than being randomly scattered in world coordinates.
 *
 * No Minecraft types here: the spatial intent and deterministic variation can
 * be tested without loading a world. The world adapter validates terrain and
 * the exact model envelope before it commits any blocks.
 */
public final class BattlefieldSceneRecipes {
    private BattlefieldSceneRecipes() {}

    public enum Scene {
        LAST_STAND("last_stand", "Последний рубеж"),
        BANNER_LINE("banner_line", "Знамённая линия");

        private final String id;
        private final String label;

        Scene(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() { return id; }
        public String label() { return label; }
    }

    public enum Prop {
        SHIELD, SPEAR, CROSSBOW, BANNER, BANNER_BARE,
        PALE_LONGSWORD, PALE_LINEAR_GREATSWORD,
        BLOOD_TALVAR, BLOOD_DUSSAK,
        SPIRE_TORTURER_GREATSWORD, SPIRE_ROARING_AXE,
        SPIRE_SHADOW_SCIMITAR, SPIRE_RAPID_KNIFE,
        SOUL_RAM_SHIELD, SOUL_REPEL_SHIELD, SOUL_MERCY_SHIELD, SOUL_RADIANT_SHIELD,
        PALADIN_BANNER, PALADIN_BANNER_BARE, HUNTER_BANNER, HUNTER_BANNER_BARE
    }

    /**
     * Relative coordinates (forward/right) are in blocks, relative to a zone
     * anchor, not to the origin of the world. Required anchors define the
     * scene's identity; optional pieces never block the core composition.
     */
    public record Slot(Prop prop, int forward, int right, int facingTurns, boolean required) {}

    public static List<Slot> plan(Scene scene, long localSeed) {
        long hash = mix(localSeed);
        int mirror = (hash & 1L) == 0L ? 1 : -1;
        int skew = (hash & 2L) == 0L ? 0 : 1;

        return switch (scene) {
            // Shield is the fixed visual anchor; the nearby spear is essential.
            // The second spear, fallen crossbow and a banner at the edge are
            // optional evidence, not hard dependencies or a fictional faction.
            case LAST_STAND -> List.of(
                    new Slot(Prop.SHIELD, 0, 0, 0, true),
                    new Slot(Prop.SPEAR, 1, 2 * mirror, 0, true),
                    new Slot(Prop.SPEAR, -1, -2 * mirror, 0, false),
                    new Slot(Prop.CROSSBOW, -2, 2 * mirror, 0, false),
                    new Slot(Prop.BANNER, -1 - skew, -4 * mirror, 0, false)
            );

            // Two distinct standing banners are an intentional *line* with
            // a gap, not a wall of flags. A shield is the linked episode;
            // an additional spear may or may not fit the available terrain.
            case BANNER_LINE -> List.of(
                    new Slot(Prop.BANNER, -1, -4 * mirror, 0, true),
                    new Slot(Prop.BANNER, 1 + skew, 4 * mirror, 0, true),
                    new Slot(Prop.SHIELD, 0, -mirror, 0, true),
                    new Slot(Prop.SPEAR, 1, 2 * mirror, 0, false)
            );
        };
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
