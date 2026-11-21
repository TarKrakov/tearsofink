import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Scene;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Slot;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free recipe tests: same anchor+seed gives the same linked scene. */
public final class BattlefieldSceneRecipesTest {
    private static void check(boolean okay, String why) {
        if (!okay) throw new AssertionError(why);
    }

    public static void main(String[] args) {
        Set<List<Slot>> variants = new HashSet<>();
        for (Scene scene : Scene.values()) {
            for (long seed = 0; seed < 256; seed++) {
                List<Slot> recipe = BattlefieldSceneRecipes.plan(scene, seed);
                check(recipe.equals(BattlefieldSceneRecipes.plan(scene, seed)), "nondeterministic recipe");
                check(!recipe.isEmpty(), "empty scene");
                Set<String> anchors = new HashSet<>();
                for (Slot slot : recipe) {
                    check(slot.prop() != null, "missing prop");
                    check(Math.abs(slot.forward()) <= 4 && Math.abs(slot.right()) <= 5,
                            "unbounded scatter");
                    check(anchors.add(slot.forward() + "," + slot.right()), "duplicate anchors");
                }
                if (scene == Scene.BANNER_LINE) {
                    long flags = recipe.stream().filter(s -> s.prop() == Prop.BANNER && s.required()).count();
                    check(flags == 2, "banner line needs both banners");
                    check(recipe.stream().anyMatch(s -> s.prop() == Prop.SHIELD && s.required()),
                            "banner line needs related weapon anchor");
                    List<Slot> banners = recipe.stream().filter(s -> s.prop() == Prop.BANNER).toList();
                    check(Math.abs(banners.get(0).right() - banners.get(1).right()) >= 6,
                            "banner line has no visual gap");
                } else {
                    check(recipe.stream().anyMatch(s -> s.prop() == Prop.SHIELD && s.required()),
                            "last stand needs shield");
                    check(recipe.stream().anyMatch(s -> s.prop() == Prop.SPEAR && s.required()),
                            "last stand needs spear");
                }
                variants.add(recipe);
            }
        }
        check(variants.size() >= 4, "no deterministic mirrored/skewed variants");
        System.out.println("Battlefield zone recipes: deterministic, bounded, linked anchors and clear gaps.");
    }
}
