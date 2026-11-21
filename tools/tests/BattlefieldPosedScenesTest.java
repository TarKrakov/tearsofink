import com.tarkrakov.tearsofink.world.battlefield.BattlefieldCompositionArchetypes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSoulShieldCatalog;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * D-127: prove different *geometric stories*, not just different item names.
 * Also regress save-compatible layout 0 and the D-126 blade-tip orientation.
 * Runnable as pure Java (no Fabric/Minecraft dependencies).
 */
public final class BattlefieldPosedScenesTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    private static double[] projected(Pose p, double height) {
        double px = Math.toRadians(p.pitch()), rz = Math.toRadians(p.roll());
        double yw = Math.toRadians(p.yaw());
        double y = height * Math.cos(px), z = height * Math.sin(px);
        double x = -y * Math.sin(rz);
        return new double[]{x * Math.cos(yw) + z * Math.sin(yw),
                -x * Math.sin(yw) + z * Math.cos(yw)};
    }

    private static void legacyContact(List<Pose> poses) {
        Pose shield = poses.stream().filter(p -> p.prop() == Prop.SHIELD && p.required())
                .findFirst().orElseThrow();
        Pose spear = poses.stream().filter(p -> p.prop() == Prop.SPEAR && p.required())
                .findFirst().orElseThrow();
        double[] a = projected(shield, 1.1);
        double[] b = projected(spear, 1.6);
        double dx = shield.right() + a[0] - (spear.right() + b[0]);
        double dz = shield.forward() - a[1] - (spear.forward() - b[1]);
        check(Math.hypot(dx, dz) < 0.9, "Historic shield/spear relation lost");
    }

    private record Space(double width, double length, double radius, int coreCount, int count) {}

    private static Space space(List<Pose> poses) {
        double minR = Double.POSITIVE_INFINITY, maxR = Double.NEGATIVE_INFINITY;
        double minF = Double.POSITIVE_INFINITY, maxF = Double.NEGATIVE_INFINITY;
        double maxRad = 0;
        int core = 0;
        for (Pose p : poses) {
            minR = Math.min(minR, p.right());
            maxR = Math.max(maxR, p.right());
            minF = Math.min(minF, p.forward());
            maxF = Math.max(maxF, p.forward());
            maxRad = Math.max(maxRad, Math.hypot(p.right(), p.forward()));
            if (p.required()) core++;
        }
        return new Space(maxR - minR, maxF - minF, maxRad, core, poses.size());
    }

    private static void validateBlades(Scenario scenario, int variant, List<Pose> poses) {
        for (Pose p : poses) {
            if (!BattlefieldWeaponCatalog.isWeapon(p.prop())) continue;
            var mesh = BattlefieldWeaponCatalog.fromProp(p.prop());
            double cos = Math.cos(Math.toRadians(p.pitch())) * Math.cos(Math.toRadians(p.roll()));
            double gripHeight = mesh.tipDownY(mesh.minX, mesh.minY) * cos + p.sink();
            double bladeHeight = mesh.tipDownY(mesh.maxX, mesh.maxY) + p.sink();
            check(gripHeight >= -0.025, "Handle buried " + scenario + "/" + variant + ": " + p.prop());
            check(bladeHeight <= 0.001, "Blade tip in air " + scenario + ": " + p.prop());
            if (Math.abs(cos) < 0.25)
                check(p.sink() > -0.12f, "Near-horizontal handle buried " + scenario);
        }
    }

    public static void main(String[] args) {
        Scenario[] scenarios = Scenario.values();
        check(scenarios.length == 40, "20 original + 14 D-131 + 6 D-132 scenes required");
        check(scenarios[0] == Scenario.LAST_STAND && scenarios[1] == Scenario.BANNER_LINE
                && scenarios[13] == Scenario.FINAL_TRACE && scenarios[33] == Scenario.MIXED_DEAD_GROUND,
                "Persisted D-125 scene IDs must remain stable (0..13)");
        check(BattlefieldCompositionArchetypes.Silhouette.values().length == 20,
                "Every scenario needs a separate meaningful silhouette class");

        Set<String> newTopologies = new HashSet<>();
        EnumSet<Prop> used = EnumSet.noneOf(Prop.class);
        int sparse = 0, expansive = 0, bladeHeavy = 0, shieldless = 0, bannerScenes = 0;
        for (Scenario scene : scenarios) {
            if (scene.ordinal() >= 20) continue; // D-127 family; historical family tested below.
            List<Pose> first = BattlefieldPosedScenes.plan(scene, 0);
            Space initial = space(first);
            String topology = first.stream().map(p -> p.prop().name() + (p.required() ? "!" : "?"))
                    .collect(java.util.stream.Collectors.joining("|"));
            check(newTopologies.add(topology), "Duplicate weapon-story topology: " + scene);
            check(initial.coreCount >= 2 && initial.count <= 16, "Broken core/contact NBT: " + scene);
            check(initial.radius <= 7.4, "Scattered props beyond episode range: " + scene);
            if (initial.count <= 3) sparse++;
            if (initial.width >= 6.0 || initial.length >= 5.3) expansive++;
            if (first.stream().filter(p -> BattlefieldWeaponCatalog.isWeapon(p.prop())).count() >= 4) bladeHeavy++;
            if (first.stream().noneMatch(p -> p.prop() == Prop.SHIELD)) shieldless++;
            if (first.stream().anyMatch(Pose::banner)) bannerScenes++;
            for (Pose p : first) used.add(p.prop());

            for (int variant = 0; variant < 8; variant++) {
                List<Pose> poses = BattlefieldPosedScenes.plan(scene, variant, 1);
                check(poses == BattlefieldPosedScenes.plan(scene, variant),
                        "New layouts must be cached and immutable");
                check(poses == BattlefieldPosedScenes.plan(scene, variant, 1),
                        "Per-frame allocations detected");
                check(poses.stream().filter(Pose::required).count() >= 2, "Core lost: " + scene);
                check(poses.size() <= 16, "Contact heights exceed BE storage limit");
                for (Pose p : poses) {
                    check(Math.abs(p.right()) <= 5.6f && Math.abs(p.forward()) <= 3.9f,
                            "Model too far away: " + scene + " " + p);
                    check(Math.abs(p.pitch()) <= 85f && Math.abs(p.roll()) <= 85f,
                            "Invalid rotation: " + scene + " " + p);
                    check(!p.banner() || (p.pitch() == 0 && p.roll() == 0),
                            "Original animated banners must stay unchanged");
                }
                validateBlades(scene, variant, poses);
                check(!poses.equals(BattlefieldPosedScenes.plan(scene, variant ^ 1)),
                        "No mirrored alternative: " + scene);
                check(!poses.equals(BattlefieldPosedScenes.plan(scene, variant ^ 4)),
                        "No substantial alternative: " + scene);
            }

            if (scene.ordinal() < 14) {
                for (int v = 0; v < 8; v++) {
                    List<Pose> legacy = BattlefieldPosedScenes.plan(scene, v, 0);
                    check(legacy == BattlefieldPosedScenes.plan(scene, v, 0),
                            "Legacy poses must remain cached");
                    check(!legacy.equals(BattlefieldPosedScenes.plan(scene, v, 1)),
                            "D-127 must not silently reuse D-125's repetitive scene: " + scene);
                    if (scene == Scenario.LAST_STAND || scene == Scenario.BANNER_LINE)
                        legacyContact(legacy);
                    validateBlades(scene, v, legacy);
                }
            }
        }

        for (var model : BattlefieldWeaponCatalog.Source.values()) {
            check(used.contains(model.prop), "Approved author source is not included: " + model);
            check(model.approximateLength() > 0.5f, "Broken model geometry: " + model);
            check(BattlefieldWeaponCatalog.fromProp(model.prop) == model, "Source ID reordered");
            check(BattlefieldWeaponCatalog.TIP_DOWN_Z_DEGREES == -135f,
                    "Common blade transform has regressed to handle-first");
            check(Math.abs(model.tipDownY(model.maxX, model.maxY)) < 0.00001,
                    "Blade head not at ground pivot");
            check(model.tipDownY(model.minX, model.minY) > 0.70,
                    "Source handle not above blade tip");
        }

        check(sparse >= 1 && expansive >= 12 && bladeHeavy >= 4
                        && shieldless >= 7 && bannerScenes >= 2,
                "Diversity metrics degenerated into the same central clutter: "
                        + sparse + " sparse, " + expansive + " wide/deep, "
                        + bladeHeavy + " blade-rich, " + shieldless + " without shields");

        // Distinctive NEGATIVE SPACE is an intentional part of each story.
        for (int v = 0; v < 8; v++) {
            for (Pose p : BattlefieldPosedScenes.plan(Scenario.BLADE_GROVE, v))
                check(Math.abs(p.right()) >= 3.0, "Weapon banks blocked the central corridor");
            for (Pose p : BattlefieldPosedScenes.plan(Scenario.TWIN_STANDARDS, v))
                check(Math.abs(p.right()) >= 2.0, "Banner gate blocked by props");
            check(space(BattlefieldPosedScenes.plan(Scenario.FINAL_TRACE, v)).width > 8.0,
                    "The sparse far traces were collapsed into a central heap");
            check(space(BattlefieldPosedScenes.plan(Scenario.HUNTER_TRACE, v)).length > 6.0,
                    "Retreat trace became a local pile");
            check(BattlefieldPosedScenes.plan(Scenario.LONE_BLADE, v).stream()
                    .filter(Pose::required).count() == 2,
                    "Lonely weapon must have only one distant required trace");
            check(BattlefieldPosedScenes.plan(Scenario.BANNER_LINE, v).stream()
                    .filter(Pose::banner).count() == 2,
                    "Banner line lost two distinct animated banners");
        }

        // D-131: separate banner-led and situation-led scenes, appended to all old IDs.
        int historicalBanner = 0, historicalBare = 0, historicalNoBanner = 0;
        EnumSet<Prop> soulShieldsSeen = EnumSet.noneOf(Prop.class);
        int dedicatedPaladinEpisodes = 0, dedicatedInkEpisodes = 0;
        Set<String> historicalTopologies = new HashSet<>();
        for (int sceneId = 20; sceneId < scenarios.length; sceneId++) {
            Scenario scene = scenarios[sceneId];
            List<Pose> p0 = BattlefieldPosedScenes.plan(scene, 0, 2);
            if (sceneId >= 34 && sceneId <= 36) {
                check(p0.stream().filter(p -> BattlefieldSoulShieldCatalog.isShield(p.prop()) && p.required()).count() >= 2,
                        "New paladin scene needs real Soul shields in its core: " + scene);
                dedicatedPaladinEpisodes++;
            }
            if (sceneId >= 37) {
                check(p0.stream().noneMatch(p -> BattlefieldSoulShieldCatalog.isShield(p.prop())),
                        "Historic Ink Hunters must not inherit Soul shields");
                dedicatedInkEpisodes++;
            }
            for (Pose p : p0) if (BattlefieldSoulShieldCatalog.isShield(p.prop())) soulShieldsSeen.add(p.prop());
            String signature = p0.stream().map(p -> p.prop() + "/" + (p.required() ? "!" : "?"))
                    .collect(java.util.stream.Collectors.joining(";"));
            check(historicalTopologies.add(signature), "Duplicate new scene topology " + scene);
            long banners = p0.stream().filter(Pose::banner).count();
            check(banners <= 1, "Generic flag spam " + scene);
            if (scene == Scenario.REGULAR_SQUAD_LINE_ANCHOR
                    || scene == Scenario.REGULAR_SQUAD_RALLY_POINT) {
                check(banners == 1 && p0.stream().anyMatch(p -> p.prop() == Prop.BANNER && p.required()),
                        "Regular squad scene missing the real full banner");
                historicalBanner++;
            } else if (scene == Scenario.REGULAR_BARE_REMNANT) {
                check(banners == 1 && p0.stream().anyMatch(p -> p.prop() == Prop.BANNER_BARE && p.required()),
                        "Regular aftermath missing the author's bare pole");
                historicalBare++;
            } else {
                check(banners == 0, "Other factions cannot inherit a fake regular banner: " + scene);
                historicalNoBanner++;
            }
            for (int variant = 0; variant < 8; variant++) {
                List<Pose> plan = BattlefieldPosedScenes.plan(scene, variant, 2);
                check(plan == BattlefieldPosedScenes.plan(scene, variant, 2)
                                && plan == BattlefieldPosedScenes.plan(scene, variant),
                        "D-131 plans must be cached, with explicit saved layout=2");
                check(plan.size() <= 16 && plan.stream().filter(Pose::required).count() >= 2,
                        "Unsafe scene NBT / missing required core: " + scene);
                check(space(plan).radius <= 7.4, "D-131 scene escaped spatial bounds: " + scene);
                for (Pose prop : plan) {
                    check(Math.abs(prop.forward()) < 3.9 && Math.abs(prop.right()) < 5.6,
                            "New scene exceeds terrain search bounds: " + scene + "/" + variant);
                    check(Math.abs(prop.pitch()) <= 85 && Math.abs(prop.roll()) <= 85,
                            "Invalid authored 3D tilt: " + scene);
                    check(!prop.banner() || (prop.pitch() == 0 && prop.roll() == 0),
                            "Banner must be an independent existing block");
                }
                validateBlades(scene, variant, plan);
                check(!plan.equals(BattlefieldPosedScenes.plan(scene, variant ^ 1, 2)),
                        "Missing mirrored new scene " + scene);
                check(!plan.equals(BattlefieldPosedScenes.plan(scene, variant ^ 4, 2)),
                        "Missing shifted fall variant " + scene);
            }
            check(BattlefieldPosedScenes.plan(scene, 3, 0).equals(BattlefieldPosedScenes.plan(scene, 3, 2)),
                    "New scene IDs should not accidentally select old recipe content");
        }
        check(historicalBanner == 2 && historicalBare == 1 && historicalNoBanner == 17
                        && dedicatedPaladinEpisodes == 3 && dedicatedInkEpisodes == 3,
                "Historical roster lost banner/no-banner balance");

        for (var soul : BattlefieldSoulShieldCatalog.Source.values()) {
            check(soulShieldsSeen.contains(soul.prop), "Unused existing Soul shield: " + soul);
            check(soul.meshIndex() >= 8 && soul.meshIndex() <= 11, "Saved weapon mesh index reassigned");
            check(soul.visibleHeight() > 2f && soul.visibleHeight() < 2.5f,
                    "Soul shield decor not scaled to x1.5");
            check(!BattlefieldWeaponCatalog.isWeapon(soul.prop), "Soul shield mistaken for blade");
        }

        // D-133: four real faction flag roles, new layout=3 only. Older layout=2
        // already exists in saved worlds: it MUST remain byte-for-byte identical.
        int paladinStandards = 0, hunterStandards = 0, paladinBare = 0, hunterBare = 0;
        for (int id = 20; id < scenarios.length; id++) {
            Scenario s = scenarios[id];
            for (int v = 0; v < 8; v++) {
                List<Pose> old = BattlefieldPosedScenes.plan(s, v, 2);
                List<Pose> now = BattlefieldPosedScenes.plan(s, v, 3);
                check(now == BattlefieldPosedScenes.plan(s, v, 3), "layout=3 must be cached");
                check(now.size() <= 16, "New faction banner overflows Contacts");
                check(old.equals(now.subList(0, old.size())), "Old scene poses reordered: " + s);
                for (Pose p : now) {
                    if (p.prop() == Prop.PALADIN_BANNER) paladinStandards++;
                    if (p.prop() == Prop.PALADIN_BANNER_BARE) paladinBare++;
                    if (p.prop() == Prop.HUNTER_BANNER) hunterStandards++;
                    if (p.prop() == Prop.HUNTER_BANNER_BARE) hunterBare++;
                    if (p.banner()) check(p.pitch() == 0 && p.roll() == 0,
                            "Independent authored banner must remain upright");
                }
                if (s == Scenario.PALADIN_HOLDOUT_ANCHOR ||
                        s == Scenario.PALADIN_BARE_RELIC ||
                        s == Scenario.HUNTER_STRIKE_MARK ||
                        s == Scenario.HUNTER_BARE_TRACE)
                    check(now.get(now.size()-1).required() && now.get(now.size()-1).banner(),
                            "Flag must anchor its named formation scene: " + s);
                if (s == Scenario.INK_HUNTER_LAST_TRACE)
                    check(now.equals(old), "Lone trace should remain bannerless");
            }
        }
        check(paladinStandards >= 8 && hunterStandards >= 8 && paladinBare >= 8 && hunterBare >= 8,
                "Four actual faction banner roles must all be used in scene recipes");

        // First twenty scene IDs and layouts are still the same public contract.
        for (int id = 0; id < 20; id++) {
            for (int v = 0; v < 8; v++) {
                check(BattlefieldPosedScenes.plan(scenarios[id], v, 1)
                        == BattlefieldPosedScenes.plan(scenarios[id], v, 2),
                        "Layout 2 unexpectedly changed old saved preview " + id);
            }
        }

        System.out.println("D-132: 6 appended faction episodes x 8 variants, four real Soul shield meshes x1.5.");
        System.out.println("D-131: 14 earlier episodes x 8 variants, 2 real full-banner scenes, "
                + "1 author bare-standard scene, 11 situation-led scenes; old IDs/layouts preserved.");
        System.out.println("D-127: 20 unique composition archetypes x 8 layouts, "
                + "14 saved versions preserved, nonuniform silhouettes, "
                + "real blade-tip contacts and 8 approved meshes.");
    }
}
