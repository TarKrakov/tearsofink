import com.tarkrakov.tearsofink.client.armor.ArmorArmFit;

public class ArmorArmFitTest {
    private static void expect(float actual, float expected, String name) {
        if (Math.abs(actual - expected) > 1e-6f) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
    public static void main(String[] args) {
        // Minecraft local coordinates: inside right-arm edge is +1 pixel, outside is -3.
        // The Alex arm must keep the inside edge, narrowing the outside by one pixel.
        expect(ArmorArmFit.slimLocalX( 1f / 16f, true),  1f / 16f, "right inside");
        expect(ArmorArmFit.slimLocalX(-3f / 16f, true), -2f / 16f, "right outside");
        expect(ArmorArmFit.slimLocalX(-1f / 16f, false), -1f / 16f, "left inside");
        expect(ArmorArmFit.slimLocalX( 3f / 16f, false),  2f / 16f, "left outside");
        expect(ArmorArmFit.slimLocalX(0, true), -ArmorArmFit.slimLocalX(0, false), "symmetry");
        expect(ArmorArmFit.SLIM_X_SCALE, 0.75f, "3px / 4px");
        System.out.println("ArmorArmFitTest OK");
    }
}
