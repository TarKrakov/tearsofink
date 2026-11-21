import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import java.util.List;

/** Tests the composition grammar without Minecraft or any artist resources. */
public final class BattlefieldPosedScenesTest {
    // Scene geometry, not only rotation: mandatory spear tip should come
    // close enough to the slanted shield's upper-mid section to read as one
    // event from the player's viewpoint. Formula matches Y -> Z -> X pivots.
    private static double[] projected(Pose p, double height) {
        double px = Math.toRadians(p.pitch()), rz = Math.toRadians(p.roll());
        double yw = Math.toRadians(p.yaw());
        double y = height * Math.cos(px), z = height * Math.sin(px);
        double x = -y * Math.sin(rz);
        return new double[]{x * Math.cos(yw) + z * Math.sin(yw),
                -x * Math.sin(yw) + z * Math.cos(yw)};
    }

    private static void linkedSpearAndShield(List<Pose> poses) {
        Pose shield = poses.stream().filter(p -> p.prop() == Prop.SHIELD && p.required())
                .findFirst().orElseThrow();
        Pose spear = poses.stream().filter(p -> p.prop() == Prop.SPEAR && p.required())
                .findFirst().orElseThrow();
        double[] a = projected(shield, 1.1);
        double[] b = projected(spear, 1.6);
        double dx = shield.right() + a[0] - (spear.right() + b[0]);
        double dz = shield.forward() - a[1] - (spear.forward() - b[1]);
        check(Math.hypot(dx, dz) < 0.9,
                "Mandatory spear no longer meets tilted shield: " + Math.hypot(dx, dz));
    }
    private static void check(boolean good, String reason) {
        if (!good) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        for (Scenario scene : Scenario.values()) {
            for (int v = 0; v < 8; v++) {
                List<Pose> a = BattlefieldPosedScenes.plan(scene, v);
                List<Pose> b = BattlefieldPosedScenes.plan(scene, v);
                check(a.equals(b), "pose layout changed without seed change");
                long tilted = a.stream().filter(p -> !p.banner() &&
                        (Math.abs(p.roll()) >= 10f || Math.abs(p.pitch()) >= 10f)).count();
                check(tilted >= 2, "only standing, cardinal-facing props");
                check(a.stream().filter(Pose::required).count() >= 2, "missing core");
                if (scene == Scenario.LAST_STAND || scene == Scenario.BANNER_LINE) linkedSpearAndShield(a);
                check(a.stream().anyMatch(p -> p.prop() == Prop.SHIELD && p.sink() < -0.2f),
                        "shield must be grounded and partly buried");
                check(a.stream().allMatch(p -> Math.abs(p.right()) <= 5f
                        && Math.abs(p.forward()) <= 2f), "scene scattered too widely");
                check(a.stream().allMatch(p -> Math.abs(p.roll()) <= 85f
                        && Math.abs(p.pitch()) <= 85f), "nonsensical tilt");
                if (scene == Scenario.BANNER_LINE || scene == Scenario.TWIN_STANDARDS) {
                    List<Pose> flags = a.stream().filter(Pose::banner).toList();
                    check(flags.size() == 2 && flags.stream().allMatch(Pose::required),
                            "line needs both banners");
                    check(Math.abs(flags.get(0).right() - flags.get(1).right()) >= 8f,
                            "line lacks spatial gap");
                    if (scene == Scenario.BANNER_LINE)
                        check(a.stream().anyMatch(p -> p.prop() == Prop.SPEAR && p.required()
                                && Math.abs(p.roll()) >= 25f),
                                "original banner line must retain its shield-spear contact");
                }
                if (scene == Scenario.LAST_STAND) {
                    check(a.stream().anyMatch(p -> p.prop() == Prop.SPEAR && p.required() &&
                            Math.abs(p.roll()) >= 25f), "original spear no longer leans");
                    check(a.stream().anyMatch(p -> p.prop() == Prop.SPEAR && p.pitch() >= 60f),
                            "original fallen spear missing");
                    check(a.stream().anyMatch(p -> p.prop() == Prop.CROSSBOW && p.pitch() > 10f),
                            "original fallen crossbow missing");
                }
                check(a.stream().noneMatch(p -> p.prop() == Prop.BANNER &&
                        (p.pitch() != 0f || p.roll() != 0f)),
                        "animated flags must retain their original upright art");
                if (scene == Scenario.BLADE_GROVE) {
                    long bladeCount = a.stream().filter(p -> BattlefieldWeaponCatalog.isWeapon(p.prop())).count();
                    check(bladeCount >= 5, "blade-grove missing its different weapon silhouettes");
                    check(a.stream().noneMatch(p -> Math.abs(p.right()) < 1.5f),
                            "blade grove must preserve deliberate central passage");
                }
                if (scene == Scenario.BLOOD_DUEL) {
                    check(a.stream().filter(p -> p.prop() == Prop.BLOOD_TALVAR).count() >= 2
                            && a.stream().anyMatch(p -> p.prop() == Prop.BLOOD_DUSSAK),
                            "blood duel lost its intersecting armaments");
                }
            }
        }
        check(!BattlefieldPosedScenes.plan(Scenario.LAST_STAND, 0)
                        .equals(BattlefieldPosedScenes.plan(Scenario.LAST_STAND, 1)),
                "variants do not change composition");
        // Every source in the explicitly authorized hunter+standard whitelist
        // must actually occur in at least one meaningful scene.
        java.util.EnumSet<Prop> used = java.util.EnumSet.noneOf(Prop.class);
        java.util.Set<String> topologies = new java.util.HashSet<>();
        for (Scenario scenario : Scenario.values()) {
            var p = BattlefieldPosedScenes.plan(scenario, 0);
            for (Pose pose : p) used.add(pose.prop());
            String structure = p.stream().map(pose -> pose.prop().name() + (pose.required() ? "!" : "?"))
                    .collect(java.util.stream.Collectors.joining("|"));
            check(topologies.add(structure), "duplicate scene topology: " + scenario);
            check(p.size() <= 16, "scene exceeds synchronized BE contact capacity");
        }
        for (var weapon : BattlefieldWeaponCatalog.Source.values()) {
            check(used.contains(weapon.prop), "Approved author model unused: " + weapon);
            check(weapon.approximateLength() > 0.5f, "Source weapon geometry invalid");
            check(BattlefieldWeaponCatalog.fromProp(weapon.prop) == weapon,
                    "mesh ID ordering or Prop mapping changed");
        }
        check(Scenario.values().length >= 14, "Not enough distinct battle episodes");
        for (Scenario scenario : Scenario.values()) {
            check(!BattlefieldPosedScenes.plan(scenario, 0).equals(BattlefieldPosedScenes.plan(scenario, 4)),
                    "scene has no substantial alternative pose: " + scenario);
        }
        System.out.println("Battlefield scenes: " + Scenario.values().length
                + " distinct episode topologies x 8 deterministic variants; 8 authentic hunter/Pale meshes.");
    }
}
