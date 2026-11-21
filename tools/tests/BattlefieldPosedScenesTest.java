import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Scene;
import java.util.List;

/** Tests the composition grammar without Minecraft or any artist resources. */
public final class BattlefieldPosedScenesTest {
    private static void check(boolean good, String reason) {
        if (!good) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        for (Scene scene : Scene.values()) {
            for (int v = 0; v < 8; v++) {
                List<Pose> a = BattlefieldPosedScenes.plan(scene, v);
                List<Pose> b = BattlefieldPosedScenes.plan(scene, v);
                check(a.equals(b), "pose layout changed without seed change");
                long tilted = a.stream().filter(p -> !p.banner() &&
                        (Math.abs(p.roll()) >= 10f || Math.abs(p.pitch()) >= 10f)).count();
                check(tilted >= 2, "only standing, cardinal-facing props");
                check(a.stream().filter(Pose::required).count() >= 2, "missing core");
                check(a.stream().anyMatch(p -> p.prop() == Prop.SHIELD && p.sink() < -0.2f),
                        "shield must be grounded and partly buried");
                check(a.stream().allMatch(p -> Math.abs(p.right()) <= 5f
                        && Math.abs(p.forward()) <= 2f), "scene scattered too widely");
                check(a.stream().allMatch(p -> Math.abs(p.roll()) <= 85f
                        && Math.abs(p.pitch()) <= 85f), "nonsensical tilt");
                if (scene == Scene.BANNER_LINE) {
                    List<Pose> flags = a.stream().filter(Pose::banner).toList();
                    check(flags.size() == 2 && flags.stream().allMatch(Pose::required),
                            "line needs both banners");
                    check(Math.abs(flags.get(0).right() - flags.get(1).right()) >= 8f,
                            "line lacks spatial gap");
                } else {
                    check(a.stream().anyMatch(p -> p.prop() == Prop.SPEAR && p.required() &&
                            Math.abs(p.roll()) >= 25f), "spear does not lean toward shield");
                    check(a.stream().anyMatch(p -> p.prop() == Prop.SPEAR && p.pitch() >= 60f),
                            "no fallen spear");
                    check(a.stream().anyMatch(p -> p.prop() == Prop.CROSSBOW && p.pitch() > 10f),
                            "crossbow still rigid");
                }
            }
        }
        check(!BattlefieldPosedScenes.plan(Scene.LAST_STAND, 0)
                        .equals(BattlefieldPosedScenes.plan(Scene.LAST_STAND, 1)),
                "variants do not change composition");
        System.out.println("Battlefield posed scenes: 16 linked layouts with off-axis tilts, buried shields and separated flags.");
    }
}
