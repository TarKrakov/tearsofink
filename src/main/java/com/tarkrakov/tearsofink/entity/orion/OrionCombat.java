package com.tarkrakov.tearsofink.entity.orion;

import java.util.List;
import java.util.function.Predicate;

/** Encounter tuning. All event times are in seconds on the authored clip clock. */
public final class OrionCombat {
    static final float HEALTH = 480, COOP_HEALTH = .55f, ARMOR = 8, TOUGHNESS = 3;
    static final float PHASE_THRESHOLD = .50f, PHASE_RATE = 1.28f, PHASE_MOVE = 1.30f;
    static final float PHASE_COOLDOWN = .70f, PHASE_DAMAGE = 1.20f, CLEAVE_DEFENSE = .75f;
    public static final float SCALE = .82f;
    static final double WALK_SPEED = .17, RUN_SPEED = .29, DASH_DISTANCE = 6.4, DODGE_DISTANCE = 2.8;
    static final double BLADE_TOLERANCE = .34, CLEAVE_RADIUS = 2.0, LIGHT_RADIUS = 2.55;
    static final int LONG_COOLDOWN = 230;
    static final float CLEAVE_BRACE_START = 1.5f, CLEAVE_IMPACT = 3.15f;
    static final int WAVE_DELAY = 12, WAVE_TICKS = 11;
    static final double WAVE_SPEED = .69, WAVE_HALF_WIDTH = 1.45;
    static final float WAVE_DAMAGE = 8;
    static final float SPIN_LIGHT = .68f;

    public enum Clip {
        IDLE(1), WALK(1.46f), RUN(1.40f), SLASH_COMBO(1.18f), SPIN_DASH(1.06f), CLEAVE(1.52f),
        DODGE_BACK(1), DODGE_RIGHT(1), DODGE_LEFT(1), EMPOWER(1), DEATH(1);

        final float rate;
        Clip(float rate) { this.rate = rate; }
        public float length() { return OrionExport.length(this); }
        float rate(boolean phaseTwo) {
            return rate * (phaseTwo && this != IDLE && this != EMPOWER && this != DEATH ? PHASE_RATE : 1);
        }
    }

    enum Technique {
        SLASH(70), SPIN(64), CLEAVE(104), DODGE(64);
        final int cooldown;
        Technique(int cooldown) { this.cooldown = cooldown; }
    }

    record Window(float from, float to, float damage) {
        boolean touches(float previous, float current) { return current >= from && previous < to; }
    }

    static final List<Window> SLASH = List.of(new Window(1.08f, 1.24f, 9),
            new Window(1.94f, 2.10f, 10), new Window(2.59f, 2.86f, 10));
    static final Window SPIN = new Window(.20f, .84f, 11);
    static final Window CLEAVE = new Window(3.10f, 3.19f, 16);

    enum Combo {
        BASIC(0, 36, 8, 0, 6, Technique.SLASH, Technique.SPIN),
        POWER(0, 40, 5, 10, 5, Technique.SPIN, Technique.CLEAVE),
        FULL(0, 52, 7, 0, 3, Technique.SLASH, Technique.SPIN, Technique.CLEAVE),
        QUICK(0, 55, 2, 8, 14, Technique.DODGE, Technique.SPIN, Technique.DODGE, Technique.SPIN, Technique.SLASH),
        DEADLY(2, 76, 3, 1, 1, Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE),
        ONSLAUGHT(2, 66, 0, 8, 5, Technique.SPIN, Technique.SPIN, Technique.SPIN, Technique.SPIN);

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
