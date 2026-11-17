package com.tarkrakov.tearsofink.weapon.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;

/** Server data, editable with a datapack and /reload. Each chain snapshots its tuning. */
public final class PaleGreatswordBalance {
    public record Sweep(float damageMultiplier, double reach, double arc, double knockback) {}
    public record SingleR(Sweep sweep, int sweepTicks, int windowTicks, int cooldownTicks,
                          double dashDistance, float dashDamageMultiplier, int dashTicks) {}
    public record Backstep(Sweep sweep, int hitTicks, int durationTicks, int cooldownTicks,
                           double backwardSpeed, double upwardSpeed) {}
    public record Tuning(SingleR singleR, Backstep backstep) {}

    private static final Tuning DEFAULT = new Tuning(
            new SingleR(new Sweep(.75f, 3.6, 140, 1.1), 14, 70, 180, 4, 1.2f, 18),
            new Backstep(new Sweep(.35f, 3, 120, .45), 3, 12, 50, .48, .12));
    private static Tuning current = DEFAULT;
    private PaleGreatswordBalance() {}
    public static Tuning get() { return current; }

    public static void init() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return TearsOfInk.id("pale_greatsword_balance"); }
            @Override public void onResourceManagerReload(ResourceManager manager) {
                var resource = manager.getResource(TearsOfInk.id("weapon_moves/pale_linear_greatsword.json"));
                if (resource.isEmpty()) { current = DEFAULT; return; }
                try (Reader reader = resource.get().openAsReader()) {
                    current = parse(JsonParser.parseReader(reader).getAsJsonObject());
                } catch (Exception e) {
                    // Keep the last valid complete tuning; never install a half-parsed configuration.
                    TearsOfInk.LOGGER.error("Pale greatsword balance not loaded; keeping previous values: {}", e.toString());
                }
            }
        });
    }

    static Tuning parse(JsonObject root) {
        JsonObject r = section(root, "single_r"), b = section(root, "backstep");
        SingleR d = DEFAULT.singleR(); Backstep k = DEFAULT.backstep();
        SingleR single = new SingleR(sweep(r, d.sweep()), ticks(r,"sweep_ticks",d.sweepTicks()),
                ticks(r,"window_ticks",d.windowTicks()), ticks(r,"cooldown_ticks",d.cooldownTicks()),
                number(r,"dash_distance",d.dashDistance(),.1,32),
                (float) number(r,"dash_damage_multiplier",d.dashDamageMultiplier(),0,100), ticks(r,"dash_ticks",d.dashTicks()));
        Backstep back = new Backstep(sweep(b,k.sweep()), ticks(b,"hit_ticks",k.hitTicks()),
                ticks(b,"duration_ticks",k.durationTicks()), ticks(b,"cooldown_ticks",k.cooldownTicks()),
                number(b,"backward_speed",k.backwardSpeed(),0,2), number(b,"upward_speed",k.upwardSpeed(),0,1));
        if (back.hitTicks() >= back.durationTicks()) throw new IllegalArgumentException("backstep hit_ticks must be before duration_ticks");
        return new Tuning(single,back);
    }
    private static Sweep sweep(JsonObject o, Sweep d) {
        return new Sweep((float) number(o,"damage_multiplier",d.damageMultiplier(),0,100),
                number(o,"reach",d.reach(),.1,32), number(o,"arc",d.arc(),1,360), number(o,"knockback",d.knockback(),0,8));
    }
    private static JsonObject section(JsonObject root, String name) {
        return root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
    }
    private static int ticks(JsonObject o, String name, int fallback) {
        double n=number(o,name,fallback,1,12000);
        if (n != Math.rint(n)) throw new IllegalArgumentException(name+" must be an integer");
        return (int)n;
    }
    private static double number(JsonObject o,String name,double fallback,double min,double max) {
        double n=o.has(name)?o.get(name).getAsDouble():fallback;
        if (!Double.isFinite(n)||n<min||n>max) throw new IllegalArgumentException(name+" outside "+min+".."+max);
        return n;
    }
}
