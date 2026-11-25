package com.tarkrakov.tearsofink.entity.orion;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static com.tarkrakov.tearsofink.entity.orion.OrionCombat.*;

public final class OrionCombatTest {
    public static void main(String[] args) {
        List<List<Technique>> steps = List.of(List.of(Technique.SLASH, Technique.SPIN), List.of(Technique.SPIN, Technique.CLEAVE), List.of(Technique.SLASH, Technique.SPIN, Technique.CLEAVE),
                List.of(Technique.DODGE, Technique.SPIN, Technique.DODGE, Technique.SPIN, Technique.SLASH), List.of(Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE, Technique.SPIN, Technique.CLEAVE),
                List.of(Technique.SPIN, Technique.SPIN, Technique.SPIN, Technique.SPIN));
        Combo[] combos = Combo.values();
        require(combos.length == steps.size(), "exactly six chains");
        for (int i = 0; i < combos.length; i++) {
            Combo combo = combos[i];
            require(combo.steps.equals(steps.get(i)), combo + " order");
            require(combo.available(false, true, move -> true) == (i < 4), combo + " phase");
            require(combo.available(true, false, move -> true) == (i < 4), combo + " long cooldown");
            require(!combo.available(true, true, move -> move != Technique.SPIN), combo + " technique cooldown at selection");
            require(combo.available(true, true, move -> true), combo + " available with ready cooldowns");
        }

        require(healthEvent(-100, HEALTH, false) == HealthEvent.EMPOWER, "burst cannot skip empower");
        require(healthEvent(HEALTH * .51f, HEALTH, false) == HealthEvent.NONE, "above phase threshold");
        require(healthEvent(HEALTH * .5f, HEALTH, true) == HealthEvent.NONE, "phase never restarts");
        require(healthEvent(0, HEALTH, true) == HealthEvent.DEFEAT, "lethal after empower");
        require(partyHealth(0) == HEALTH && partyHealth(1) == HEALTH, "solo health");
        require(partyHealth(2) == 630 && partyHealth(3) == 780 && partyHealth(4) == 930, "tested party sizes");
        require(partyHealth(5) == 958 && partyHealth(8) == 1001 && partyHealth(Integer.MAX_VALUE) == VANILLA_HEALTH_CAP,
                "larger parties taper toward the vanilla attribute cap");
        for (int players = 1; players <= 20; players++)
            require(partyHealth(players) >= partyHealth(players - 1), "party health never drops with another fighter");
        require(!acceptsDamage(true, false, true, Clip.CLEAVE), "guard between threshold and empower");
        require(!acceptsDamage(true, false, false, Clip.EMPOWER), "empower immunity");
        require(!acceptsDamage(true, true, false, Clip.DEATH), "finale immunity");
        require(incoming(20, Clip.CLEAVE, 1.49f) == 20 && incoming(20, Clip.CLEAVE, 2.4f) == 15
                && incoming(20, Clip.CLEAVE, CLEAVE_IMPACT) == 20, "cleave brace interval");

        for (boolean phaseTwo : new boolean[] {false, true}) {
            for (Window window : OrionCombat.SLASH) assertWindow(Clip.SLASH_COMBO, phaseTwo, window);
            assertWindow(Clip.CLEAVE, phaseTwo, CLEAVE);
            assertWindow(Clip.SPIN_DASH, phaseTwo, SPIN);
            assertCrossing(Clip.CLEAVE, phaseTwo, CLEAVE_IMPACT);
            assertCrossing(Clip.SPIN_DASH, phaseTwo, SPIN_LIGHT);
            assertCrossing(Clip.DEATH, phaseTwo, Clip.DEATH.length());
            assertCrossing(Clip.EMPOWER, phaseTwo, Clip.EMPOWER.length());
        }
        require(Clip.DEATH.length() == 6 && Clip.DEATH.rate(true) == 1
                && Clip.EMPOWER.length() == 2.5 && Clip.EMPOWER.rate(true) == 1, "authored finale lengths");
        require(movementProgress(DASH_START, DASH_START, DASH_END) == 0
                && movementProgress(DASH_END, DASH_START, DASH_END) == 1, "dash translation stays inside the spin");
        require(LINK_WAIT * ALIGN_TURN + ALIGN_TOLERANCE >= 180, "link can face a target behind the completed dash");
        require(dashDistance(4, false) > 4 && dashDistance(16, true) > 8, "dash travels through a nearby target");
        for (Window window : SLASH)
            require(slashAdvance(window.from()) == slashAdvance(window.to()), "rush and facing settle for each slash");
        require(slashAdvance(.35f) == 0 && Math.abs(1 - slashAdvance(2.9f)) < .0001, "three advancing windups");
        verifyAnimationPlayback();
        verifyMovementCollision();
        verifyFirstDeadly();
        verifyHitWindow();
        var cleave = OrionBlade.sample(Clip.CLEAVE, CLEAVE_IMPACT, Vec3.ZERO, 0);
        require(cleave.tip().y < .65 && cleave.tip().z > 2.4, "cleave's visible ground contact");
        var slash = OrionBlade.sample(Clip.SLASH_COMBO, 2, Vec3.ZERO, 90);
        require(slash.tip().x < -2.7 && Math.abs(slash.tip().z) < .2, "blade respects entity heading");
        require(slash.touches(new AABB(-2.9, 0, -.35, -2.4, 1.8, .35)), "visible blade reaches player");
        require(!slash.touches(new AABB(2.4, 0, -.35, 2.9, 1.8, .35)), "blade does not hit behind");
        System.out.println("Orion combat checks passed");
    }

    private static void verifyAnimationPlayback() {
        var clock = new OrionAnimationClock();
        clock.reset(0);
        for (int serverTick : new int[] {1, 2, 2, 4, 5, 5, 7, 8, 8, 10}) {
            float last = clock.current();
            clock.advance(serverTick);
            var pose = new OrionEntity.Performance(Clip.SPIN_DASH, 30, 1, Clip.IDLE, 0, false,
                    clock.previous(), clock.current(), 0);
            require(pose.elapsedTicks(400, 0) == last, "adjacent client ticks share an endpoint");
            require(pose.elapsedTicks(400, .25f) < pose.elapsedTicks(400, .75f),
                    "the visual pose advances between renders even with irregular packets");
            require(clock.current() <= serverTick + 1, "client stays within one tick of its latest sample");
        }
        for (int stalledTick = 0; stalledTick < 20; stalledTick++) clock.advance(10);
        require(clock.current() <= 11 && Math.abs(clock.current() - clock.previous()) < .001,
                "prolonged stall holds the cursor");
        clock.advance(14);
        require(clock.current() > clock.previous() && clock.current() - clock.previous() <= 1.5,
                "a late packet resumes playback without jumping");
        clock.reset(0);
        require(clock.previous() == 0 && clock.current() == 0, "the next clip starts with its own cursor");
    }

    private static void verifyMovementCollision() {
        double[] at = {0};
        moveInSlices(new Vec3(1.37, 0, 0), slice -> {
            require(slice.x <= .18, "swept subdivision");
            if (at[0] + slice.x >= .59) return false;
            at[0] += slice.x;
            return true;
        });
        require(at[0] > .45 && at[0] < .59, "stops at a blocked intermediate position");
    }

    private static void verifyFirstDeadly() {
        for (boolean burst : new boolean[] {false, true}) {
            var opening = new DeadlyOpening();
            require(!opening.required(false) && opening.required(true), "first deadly opens in phase two");
            if (burst) opening.deferLethal();
            for (Technique step : Combo.DEADLY.steps) {
                int before = opening.performed();
                require(opening.next() == step, "picks each mandatory step");
                opening.finish(step, false);
                require(opening.performed() == before && !opening.releaseLethal(), "blocked step does not advance");
                opening.finish(step, true);
                require(opening.performed() == before + 1, "completed step counts once");
            }
            require(opening.awaitingWave(), "last cleave wave stays active");
            opening.resolve(true);
            require(opening.required(true) && !opening.releaseLethal(), "wave keeps lethal deferred");
            opening.resolve(false);
            require(!opening.required(true) && opening.releaseLethal() == burst, "finale after six steps and wave");
        }
        require(new DeadlyOpening().performed() == 0, "a new encounter has its own opening");
    }

    private static void verifyHitWindow() {
        var target = new UUID(0, 1);
        var window = new HitWindow();
        require(window.register(target), "first sample hits");
        require(window.contains(target) && !window.register(target), "overlap does not duplicate damage");
        require(window.register(new UUID(0, 2)), "distinct target can be hit");
        window.reset();
        require(window.register(target) && new HitWindow().register(target), "next hit and distinct wave stay independent");
    }

    private static void assertWindow(Clip clip, boolean phaseTwo, Window window) {
        assertCrossing(clip, phaseTwo, window.from());
        assertCrossing(clip, phaseTwo, window.to());
        require(!window.touches(0, window.from() - .001f) && window.touches(0, window.to() + .001f)
                && !window.touches(window.to(), window.to() + .001f), "window at slow/fast samples " + window);
    }

    private static void assertCrossing(Clip clip, boolean phaseTwo, float event) {
        int count = 0;
        float last = 0;
        for (int tick = 1; tick <= 160; tick++) {
            float time = Math.min(tick * clip.rate(phaseTwo) / 20, clip.length());
            if (crossed(last, time, event)) count++;
            last = time;
        }
        require(count == 1, clip + " event " + event);
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
