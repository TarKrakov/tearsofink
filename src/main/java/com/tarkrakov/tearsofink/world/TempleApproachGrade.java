package com.tarkrakov.tearsofink.world;

import java.util.Arrays;

/** Exact bounded grading: at most one block per step, at most three blocks of cut/fill. */
final class TempleApproachGrade {
    private static final int CUT = 3, INF = 1_000_000;

    private TempleApproachGrade() {}

    /** Returns the least total absolute cut/fill, or null if no legal grade exists. */
    static int[] solve(int start, int end, int[] ground) {
        return solve(start,end,ground,null,null);
    }
    static int[] solve(int start,int end,int[] ground,int[] lower,int[] upper) {
        return solve(start,end,ground,lower,upper,CUT);
    }
    static int[] solve(int start,int end,int[] ground,int[] lower,int[] upper,int radius) {
        int STATES=2*radius+1;
        int CUT=radius;
        if (ground.length == 0) return start == end ? new int[0] : null;
        int n = ground.length;
        int[][] cost = new int[n][STATES];
        for (int[] row : cost) Arrays.fill(row, INF);
        for (int i = n - 1; i >= 0; i--) {
            for (int k = 0; k < STATES; k++) {
                int y = ground[i] + k - CUT;
                if ((lower!=null && y<lower[i]) || (upper!=null && y>upper[i])) continue;
                int change = Math.abs(k - CUT);
                if (i == n - 1) {
                    if (y == end) cost[i][k] = change;
                } else {
                    for (int d = -1; d <= 1; d++) {
                        int next = y + d - ground[i + 1] + CUT;
                        if (next >= 0 && next < STATES && cost[i + 1][next] < INF) {
                            cost[i][k] = Math.min(cost[i][k], change + cost[i + 1][next]);
                        }
                    }
                }
            }
        }
        int[] result = new int[n];
        int previous = start;
        for (int i = 0; i < n; i++) {
            int best = -1;
            for (int k = 0; k < STATES; k++) {
                if (cost[i][k] >= INF || Math.abs(ground[i] + k - CUT - previous) > 1) continue;
                if (best < 0 || cost[i][k] < cost[i][best]
                        || cost[i][k] == cost[i][best] && Math.abs(k - CUT) < Math.abs(best - CUT)) best = k;
            }
            if (best < 0) return null;
            result[i] = previous = ground[i] + best - CUT;
        }
        return result;
    }
}
