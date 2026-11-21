import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Deterministic aged copies of battlefield art. Original item and entity atlases are read-only. */
public final class GenerateAgedTextures {
    private static final String[] ITEM = {
        "pale_longsword", "pale_linear_greatsword", "blood_talvar", "blood_dussak",
        "spire_torturer_greatsword", "spire_roaring_axe", "spire_shadow_scimitar", "spire_rapid_knife",
        "ram_shield", "repel_shield", "mercy_shield", "radiant_shield"
    };
    private static final String[] ENTITY = {"pale_spearman", "pale_shieldbearer", "pale_crossbowman"};
    private GenerateAgedTextures() {}
    private static int noise(int x, int y, int seed) {
        int v = x * 0x1f123bb5 ^ y * 0x54adf003 ^ seed * 0x3c6ef35f;
        v ^= v >>> 16; v *= 0x7feb352d; v ^= v >>> 15; v *= 0x846ca68b;
        return (v ^ v >>> 16) & 255;
    }
    private static int cap(double a) { return Math.max(0, Math.min(255, (int)Math.round(a))); }
    private static void crack(BufferedImage image, int x, int y, double amount, boolean soul) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
        int p = image.getRGB(x, y), a = p >>> 24;
        if (a == 0) return;
        int r = cap(((p >>> 16) & 255) * (1 - amount) + (soul ? 56 : 84) * amount);
        int g = cap(((p >>> 8) & 255) * (1 - amount) + (soul ? 65 : 49) * amount);
        int b = cap((p & 255) * (1 - amount) + (soul ? 72 : 33) * amount);
        image.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
    }
    private static BufferedImage transform(BufferedImage source, int seed, boolean soul) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = source.getRGB(x, y), a = p >>> 24;
            if (a == 0) { output.setRGB(x, y, p); continue; }
            double r = (p >>> 16) & 255, g = (p >>> 8) & 255, b = p & 255;
            double gray = r * .29 + g * .58 + b * .13;
            double desat = soul ? .22 : .28;
            r += (gray - r) * desat; g += (gray - g) * desat; b += (gray - b) * desat;
            double patch = (noise(x / 5, y / 5, seed) + noise((x + 7) / 9, (y + 3) / 9, seed ^ 39)) / 510.0;
            double grain = (noise(x, y, seed ^ 101) - 127) / 255.0;
            double wear = Math.max(0, patch - .42) * (soul ? .42 : .65);
            double shade = (soul ? .81 : .73) - wear + grain * .10;
            r *= shade; g *= shade; b *= shade;
            if (patch > .59) {
                double t = Math.min(soul ? .30 : .48, (patch - .59) * 1.8);
                r = r * (1 - t) + (soul ? 80 : 119) * t;
                g = g * (1 - t) + (soul ? 92 : 68) * t;
                b = b * (1 - t) + (soul ? 106 : 40) * t;
            }
            boolean edge = x > 0 && y > 0 && x + 1 < w && y + 1 < h
                    && ((source.getRGB(x-1,y) >>> 24) == 0 || (source.getRGB(x+1,y) >>> 24) == 0
                    || (source.getRGB(x,y-1) >>> 24) == 0 || (source.getRGB(x,y+1) >>> 24) == 0);
            if (edge && noise(x, y, seed ^ 82) > 155) { r *= .70; g *= .69; b *= .68; }
            output.setRGB(x, y, a << 24 | cap(r) << 16 | cap(g) << 8 | cap(b));
        }
        // Local diagonal hairline fissures and chipped-looking dark gouges. Alpha and UV preserved.
        int strokes = Math.max(2, Math.min(12, w * h / 500));
        for (int n = 0; n < strokes; n++) {
            int x = noise(n, 31, seed) * w / 256, y = noise(n, 97, seed ^ 7) * h / 256;
            int len = 2 + Math.min(6, Math.min(w, h) / 10);
            int dir = (noise(n, 39, seed) & 1) == 0 ? 1 : -1;
            for (int i = 0; i < len; i++) {
                x += dir;
                if ((i & 1) == 1) y += (noise(n+i, i, seed) & 1) == 0 ? 1 : -1;
                crack(output, x, y, soul ? .62 : .73, soul);
                if (i % 3 == 0) crack(output, x, y + 1, soul ? .26 : .35, soul);
            }
        }
        return output;
    }
    private static void copy(String type, String name, Path source, Path dest) throws IOException {
        Path input = source.resolve(type).resolve(name + ".png");
        BufferedImage image = ImageIO.read(input.toFile());
        if (image == null) throw new IOException("Invalid authored atlas: " + input);
        BufferedImage old = transform(image, name.hashCode(), name.endsWith("_shield"));
        Path output = dest.resolve("assets/tears-of-ink/textures").resolve(type).resolve(name + "_aged.png");
        Files.createDirectories(output.getParent());
        if (!ImageIO.write(old, "PNG", output.toFile())) throw new IOException("PNG writer absent");
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("Expected texture source dir and generated resources dir");
        Path src = Path.of(args[0]), dst = Path.of(args[1]);
        for (String name : ITEM) copy("item", name, src, dst);
        for (String name : ENTITY) copy("entity", name, src, dst);
        System.out.println("Generated " + (ITEM.length + ENTITY.length) + " aged decorative atlases; source art untouched.");
    }
}
