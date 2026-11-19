package com.tarkrakov.tearsofink.world;

import java.util.Arrays;
import java.util.Random;

/** Standalone Java 21 test, no Minecraft dependencies. */
public final class TempleApproachGradeTest {
    private static final int NO = 1_000_000;

    private static int brute(int previous, int end, int[] ground, int i) {
        if (i == ground.length) return previous == end ? 0 : NO;
        int best = NO;
        for (int y = previous - 1; y <= previous + 1; y++) {
            int cut = Math.abs(y - ground[i]);
            if (cut <= 3) best = Math.min(best, cut + brute(y, end, ground, i + 1));
        }
        return best;
    }

    private static void check(int start, int end, int[] ground) {
        int expected = brute(start, end, ground, 0);
        int[] actual = TempleApproachGrade.solve(start, end, ground);
        if (expected >= NO) {
            if (actual != null) throw new AssertionError("Accepted impossible profile");
            return;
        }
        if (actual == null || actual.length != ground.length) throw new AssertionError("Missed feasible profile");
        int previous = start, cost = 0;
        for (int i = 0; i < actual.length; i++) {
            if (Math.abs(actual[i] - previous) > 1 || Math.abs(actual[i] - ground[i]) > 3)
                throw new AssertionError("Invalid step: " + Arrays.toString(actual));
            cost += Math.abs(actual[i] - ground[i]);
            previous = actual[i];
        }
        if (previous != end || cost != expected || !Arrays.equals(actual, TempleApproachGrade.solve(start, end, ground)))
            throw new AssertionError("Wrong endpoint, cost or determinism");
    }

    public static void main(String[] args) {
        check(0, 0, new int[]{0, 0, 6, 0, 0, 0}); // Greedy grading misses this feasible ramp.
        check(0, 0, new int[]{0, 7, 0});
        check(1, 1, new int[0]);
        check(1, 2, new int[0]);
        check(10, 10, new int[]{10, 10, 10});
        Random random = new Random(777);
        for (int trial = 0; trial < 20_000; trial++) {
            int[] ground = new int[1 + random.nextInt(8)];
            for (int i = 0; i < ground.length; i++) ground[i] = random.nextInt(13) - 6;
            check(random.nextInt(9) - 4, ground[ground.length - 1], ground);
        }
        System.out.println("Temple approach grading: 20,005 cases passed against exhaustive search");
    }
}
