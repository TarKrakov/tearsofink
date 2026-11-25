package com.tarkrakov.tearsofink.entity.orion;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        require(!acceptsDamage(true, false, true, Clip.CLEAVE), "guard between threshold and empower");
        require(!acceptsDamage(true, false, false, Clip.EMPOWER), "empower immunity");
        require(!acceptsDamage(true, true, false, Clip.DEATH), "finale immunity");
        require(incoming(20, Clip.CLEAVE, 1.49f) == 20 && incoming(20, Clip.CLEAVE, 2.4f) == 15
                && incoming(20, Clip.CLEAVE, CLEAVE_IMPACT) == 20, "cleave brace interval");

        for (boolean phaseTwo : new boolean[] {false, true}) {
            for (Window window : OrionCombat.SLASH) assertCrossing(Clip.SLASH_COMBO, phaseTwo, window.from());
            assertCrossing(Clip.CLEAVE, phaseTwo, CLEAVE_IMPACT);
            assertCrossing(Clip.SPIN_DASH, phaseTwo, SPIN_LIGHT);
            assertCrossing(Clip.DEATH, phaseTwo, Clip.DEATH.length());
            assertCrossing(Clip.EMPOWER, phaseTwo, Clip.EMPOWER.length());
        }
        require(Clip.DEATH.length() == 6 && Clip.DEATH.rate(true) == 1
                && Clip.EMPOWER.length() == 2.5 && Clip.EMPOWER.rate(true) == 1, "authored finale lengths");
        require(movementProgress(.12f, .12f, .82f) == 0 && movementProgress(.82f, .12f, .82f) == 1,
                "dash translation starts and ends inside the spin");
        var cleave = OrionBlade.sample(Clip.CLEAVE, CLEAVE_IMPACT, Vec3.ZERO, 0);
        require(cleave.tip().y < .65 && cleave.tip().z > 2.4, "cleave's visible ground contact");
        var slash = OrionBlade.sample(Clip.SLASH_COMBO, 2, Vec3.ZERO, 90);
        require(slash.tip().x < -2.7 && Math.abs(slash.tip().z) < .2, "blade respects entity heading");
        require(slash.touches(new AABB(-2.9, 0, -.35, -2.4, 1.8, .35)), "visible blade reaches player");
        require(!slash.touches(new AABB(2.4, 0, -.35, 2.9, 1.8, .35)), "blade does not hit behind");
        System.out.println("Orion combat checks passed");
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
