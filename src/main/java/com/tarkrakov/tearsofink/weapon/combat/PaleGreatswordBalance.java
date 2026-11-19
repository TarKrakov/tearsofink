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
    public record DualR(int cooldownTicks, double upwardSpeed, double forwardSpeed,
                        float slamDamageMultiplier, double slamReach, double slamKnockback) {}
    public record DualCharge(int cooldownTicks, double dashDistance, float minDamageMultiplier,
                             float maxDamageMultiplier) {}
    /** Toggleable forward-facing guard; reduction is the blocked share of incoming damage. */
    public record ShiftGuard(float singleReduction, float dualReduction, double singleArc, double dualArc,
                             int singleCooldownTicks, int dualCooldownTicks) {}
    public record Tuning(SingleR singleR, Backstep backstep, DualR dualR, DualCharge dualCharge,
                         ShiftGuard shiftGuard) {}

    private static final Tuning DEFAULT = new Tuning(
            new SingleR(new Sweep(.70f, 3.4, 125, 1.0), 14, 48, 160, 4, 1.10f, 18),
            new Backstep(new Sweep(.35f, 2.8, 110, .45), 3, 12, 70, .42, .12),
            new DualR(240, .92, .30, 1.25f, 3.1, .6),
            new DualCharge(260, 5.5, 1.10f, 1.8f),
            new ShiftGuard(.45f, .60f, 120, 150, 110, 160));
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
        JsonObject p = section(root, "dual_r"), c = section(root, "dual_charge"),
                g = section(root, "shift_guard");
        SingleR d = DEFAULT.singleR(); Backstep k = DEFAULT.backstep();
        DualR pd = DEFAULT.dualR(); DualCharge cd = DEFAULT.dualCharge();
        ShiftGuard gd = DEFAULT.shiftGuard();
        SingleR single = new SingleR(sweep(r, d.sweep()), ticks(r,"sweep_ticks",d.sweepTicks()),
                ticks(r,"window_ticks",d.windowTicks()), ticks(r,"cooldown_ticks",d.cooldownTicks()),
                number(r,"dash_distance",d.dashDistance(),.1,32),
                (float) number(r,"dash_damage_multiplier",d.dashDamageMultiplier(),0,100), ticks(r,"dash_ticks",d.dashTicks()));
        Backstep back = new Backstep(sweep(b,k.sweep()), ticks(b,"hit_ticks",k.hitTicks()),
                ticks(b,"duration_ticks",k.durationTicks()), ticks(b,"cooldown_ticks",k.cooldownTicks()),
                number(b,"backward_speed",k.backwardSpeed(),0,2), number(b,"upward_speed",k.upwardSpeed(),0,1));
        DualR dualR = new DualR(ticks(p,"cooldown_ticks",pd.cooldownTicks()),
                number(p,"upward_speed",pd.upwardSpeed(),.1,2),
                number(p,"forward_speed",pd.forwardSpeed(),0,2),
                (float) number(p,"slam_damage_multiplier",pd.slamDamageMultiplier(),0,100),
                number(p,"slam_reach",pd.slamReach(),.1,32),
                number(p,"slam_knockback",pd.slamKnockback(),0,8));
        DualCharge dualCharge = new DualCharge(ticks(c,"cooldown_ticks",cd.cooldownTicks()),
                number(c,"dash_distance",cd.dashDistance(),.1,32),
                (float) number(c,"min_damage_multiplier",cd.minDamageMultiplier(),0,100),
                (float) number(c,"max_damage_multiplier",cd.maxDamageMultiplier(),0,100));
        ShiftGuard guard = new ShiftGuard(
                (float) number(g,"single_reduction",gd.singleReduction(),0,.95),
                (float) number(g,"dual_reduction",gd.dualReduction(),0,.95),
                number(g,"single_arc",gd.singleArc(),1,360),
                number(g,"dual_arc",gd.dualArc(),1,360),
                ticks(g,"single_cooldown_ticks",gd.singleCooldownTicks()),
                ticks(g,"dual_cooldown_ticks",gd.dualCooldownTicks()));
        if (back.hitTicks() >= back.durationTicks()) throw new IllegalArgumentException("backstep hit_ticks must be before duration_ticks");
        if (dualCharge.minDamageMultiplier() > dualCharge.maxDamageMultiplier())
            throw new IllegalArgumentException("dual_charge min_damage_multiplier must not exceed max_damage_multiplier");
        return new Tuning(single, back, dualR, dualCharge, guard);
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
