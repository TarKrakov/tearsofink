package com.tarkrakov.tearsofink.entity.orion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.phys.Vec3;

/** Encounter tuning. All event times are in seconds on the authored clip clock. */
public final class OrionCombat {
    static final int HEALTH = 480, COOP_HEALTH_STEP = 150, COOP_FULL_STEPS = 3, VANILLA_HEALTH_CAP = 1024;
    static final double COOP_HEALTH_TAPER = .70;
    static final float ARMOR = 8, TOUGHNESS = 3;
    static final float PHASE_THRESHOLD = .50f, PHASE_RATE = 1.42f, PHASE_MOVE = 1.43f;
    static final float PHASE_COOLDOWN = .60f, CLEAVE_DEFENSE = .75f;
    public static final float SCALE = .82f;
    static final double WALK_SPEED = .19, RUN_SPEED = .31, DASH_DISTANCE = 8.6, DODGE_DISTANCE = 2.8;
    static final double BLADE_TOLERANCE = .34, CLEAVE_RADIUS = 2.0, LIGHT_RADIUS = 2.55;
    static final double SLASH_ADVANCE = 2.35, SLASH_ADVANCE_PHASE_TWO = 3.20;
    static final double SLASH_STOP_RANGE = 1.80, SLASH_START_RANGE = 5.3;
    static final double SLASH_MARGIN = .58, SPIN_MARGIN = .51;
    static final double SPIN_LIGHT_RADIUS = 1.9125;
    static final double DASH_MIN = 2.7, DASH_THROUGH = .85, DASH_CONFIRM = .75;
    static final double ENGAGE_RANGE = 14, ENGAGE_RANGE_PHASE_TWO = 17;
    static final float SLASH_TURN = 8, ALIGN_TURN = 22, ALIGN_TOLERANCE = 23, SWEEP_STEP = .015f;
    static final float DASH_START = .11f, DASH_END = .94f;
    static final int LONG_COOLDOWN = 230, OPENING_RESPONSE = 6, LINK_WAIT = 8;
    static final int SOLO_RECOVERY = 23, BLOCKED_RECOVERY = 8, REPOSITION_TICKS = 13;
    static final int OPENING_RECOVERY_BONUS = 8;
    static final float RECOVERY_PHASE_TWO = .73f;
    static final float CLEAVE_BRACE_START = 1.5f, CLEAVE_IMPACT = 3.15f;
    static final int WAVE_DELAY = 12, WAVE_TICKS = 11;
    static final double WAVE_SPEED = .69, WAVE_HALF_WIDTH = 1.45;
    static final Damage WAVE_DAMAGE = new Damage(5.6f, 6f), CLEAVE_IMPACT_DAMAGE = new Damage(7.7f, 8.25f);
    static final float SPIN_LIGHT_DAMAGE = 6f;
    static final float SPIN_LIGHT = .68f;
    static final float EMPOWER_AFTER_SLASH = 3.04f, EMPOWER_AFTER_SPIN = 1.12f;
    // Retain the pre-existing softer blade contact in DEADLY, without runtime scaling.
    static final float DEADLY_CLEAVE_DAMAGE = 10.56f;

    public enum Clip {
        IDLE(1), WALK(1.52f), RUN(1.50f), SLASH_COMBO(1.36f), SPIN_DASH(1.10f), CLEAVE(1.57f),
        DODGE_BACK(1), DODGE_RIGHT(1), DODGE_LEFT(1), EMPOWER(1), DEATH(1);

        final float rate;
        Clip(float rate) { this.rate = rate; }
        public float length() { return OrionExport.length(this); }
        boolean usesCursor() { return this != IDLE && this != WALK && this != RUN; }
        float rate(boolean phaseTwo) {
            return rate * (phaseTwo && this != IDLE && this != EMPOWER && this != DEATH ? PHASE_RATE : 1);
        }
    }

    enum Technique {
        SLASH(59), SPIN(52), CLEAVE(87), DODGE(57);
        final int cooldown;
        Technique(int cooldown) { this.cooldown = cooldown; }
    }

    record Damage(float first, float second) {
        float at(boolean phaseTwo) { return phaseTwo ? second : first; }
    }

    record Window(float from, float to, Damage damage) {
        boolean touches(float previous, float current) { return current >= from && previous < to; }
    }

    static final List<Window> SLASH = List.of(new Window(1.05f, 1.28f, new Damage(6.3f, 6.75f)),
            new Window(1.91f, 2.14f, new Damage(7f, 7.5f)), new Window(2.60f, 2.89f, new Damage(7f, 7.5f)));
    static final Window SPIN = new Window(.22f, .94f, new Damage(6.65f, 7.125f));
    static final Window CLEAVE = new Window(3.10f, 3.19f, new Damage(11.2f, 12f));

    enum Combo {
        BASIC(0, 29, 8, 0, 6, Technique.SLASH, Technique.SPIN),
        POWER(0, 31, 5, 10, 5, Technique.SPIN, Technique.CLEAVE),
        FULL(0, 39, 7, 0, 3, Technique.SLASH, Technique.SPIN, Technique.CLEAVE),
        QUICK(0, 42, 2, 8, 14, Technique.DODGE, Technique.SPIN, Technique.DODGE, Technique.SPIN, Technique.SLASH),
        DEADLY(2, 55, 3, 1, 1, Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE),
        ONSLAUGHT(2, 51, 0, 8, 5, Technique.SPIN, Technique.SPIN, Technique.SPIN, Technique.SPIN);

        final int phase, recovery, close, distant, mobile;
        final List<Technique> steps;
        Combo(int phase, int recovery, int close, int distant, int mobile, Technique... steps) {
            this.phase = phase;
            this.recovery = recovery;
            this.close = close;
            this.distant = distant;
            this.mobile = mobile;
            this.steps = List.of(steps);
        }

        boolean available(boolean phaseTwo, boolean longReady, Predicate<Technique> ready) {
            return (phase != 2 || phaseTwo && longReady) && steps.stream().allMatch(ready);
        }
    }

    /** The first chain is independent of weighted selection and retains its next step across obstructions. */
    static final class DeadlyOpening {
        private int performed;
        private boolean completed;
        private boolean deferredLethal;

        int performed() { return performed; }
        boolean required(boolean phaseTwo) { return phaseTwo && !completed; }
        boolean awaitingWave() { return performed == Combo.DEADLY.steps.size() && !completed; }

        Technique next() { return Combo.DEADLY.steps.get(performed); }

        void finish(Technique move, boolean executed) {
            if (executed && !awaitingWave() && !completed && move == next()) performed++;
        }

        void resolve(boolean waveActive) {
            if (awaitingWave() && !waveActive) completed = true;
        }

        void deferLethal() { deferredLethal = true; }
        boolean lethalDeferred() { return deferredLethal; }
        boolean releaseLethal() { return completed && deferredLethal; }
    }

    static final class HitWindow {
        private final Set<UUID> hit = new HashSet<>();

        boolean contains(UUID target) { return hit.contains(target); }
        boolean register(UUID target) { return hit.add(target); }
        void reset() { hit.clear(); }
    }

    static void moveInSlices(Vec3 delta, Predicate<Vec3> move) {
        int slices = Math.max(1, (int)Math.ceil(delta.horizontalDistance() / .18));
        Vec3 slice = delta.scale(1.0 / slices);
        for (int i = 0; i < slices; i++) {
            if (!move.test(slice)) return;
        }
    }

    static int partyHealth(int players) {
        int extra = Math.max(1, players) - 1;
        int fullSteps = Math.min(extra, COOP_FULL_STEPS);
        int health = HEALTH + fullSteps * COOP_HEALTH_STEP;
        // Larger parties use a diminishing share of the remaining vanilla attribute range.
        double share = 1 - Math.pow(COOP_HEALTH_TAPER, extra - fullSteps);
        return health + (int)Math.round((VANILLA_HEALTH_CAP - health) * share);
    }

    static double dashDistance(double targetDistance, boolean phaseTwo) {
        double limit = DASH_DISTANCE * (phaseTwo ? PHASE_MOVE / PHASE_RATE : 1);
        return Math.clamp(targetDistance + DASH_THROUGH, DASH_MIN, limit);
    }

    static float slashAdvance(float time) {
        return .50f * movementProgress(time, .38f, 1.04f)
                + .29f * movementProgress(time, 1.46f, 1.89f)
                + .21f * movementProgress(time, 2.18f, 2.56f);
    }

    enum HealthEvent { NONE, EMPOWER, DEFEAT }

    static HealthEvent healthEvent(float nextHealth, float maxHealth, boolean phaseTwo) {
        if (!phaseTwo && nextHealth <= maxHealth * PHASE_THRESHOLD) return HealthEvent.EMPOWER;
        return nextHealth <= 0 ? HealthEvent.DEFEAT : HealthEvent.NONE;
    }

    static boolean acceptsDamage(boolean fighting, boolean pendingLethal, boolean pendingEmpower, Clip clip) {
        return fighting && !pendingLethal && !pendingEmpower && clip != Clip.EMPOWER && clip != Clip.DEATH;
    }

    static float incoming(float amount, Clip clip, float seconds) {
        return clip == Clip.CLEAVE && seconds >= CLEAVE_BRACE_START && seconds < CLEAVE_IMPACT
                ? amount * CLEAVE_DEFENSE : amount;
    }

    static boolean crossed(float previous, float current, float event) {
        return previous < event && current >= event;
    }

    static float movementProgress(float seconds, float start, float end) {
        float fraction = Math.clamp((seconds - start) / (end - start), 0, 1);
        return fraction * fraction * (3 - 2 * fraction);
    }

    private OrionCombat() {}
}
