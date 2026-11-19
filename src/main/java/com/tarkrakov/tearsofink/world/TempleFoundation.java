package com.tarkrakov.tearsofink.world;

final class TempleFoundation {
    static final int APRON=10, MAX_FILL=10, PATH_SUPPORT=7;
    private TempleFoundation() {}
    static int distance(int dx,int dz,int hx,int hz) {
        return Math.max(Math.max(0,Math.abs(dx)-hx),Math.max(0,Math.abs(dz)-hz));
    }
    static int top(int base,int distance) { return base-1-distance; }
    /** A solid cap above the original floor cannot terminate the fill. */
    static boolean stopAtBed(int y,int bed,boolean solid) {
        return solid && y<=bed;
    }
    // Convert exactly the existing per-column fit conditions to a base-Y
    // interval. The apron imposes a support limit, not a minimum altitude.
    static int minimumBase(int surface,boolean water,int distance) {
        return distance==0 ? (water?surface+2:surface-3) : Integer.MIN_VALUE;
    }
    static int maximumBase(int bed,int distance) {
        return bed+MAX_FILL+1+distance;
    }
    static boolean fits(int top,int surface,int bed,boolean water,boolean core) {
        if (core && water && top<=surface) return false;
        if (core && surface-top>4) return false;
        return top-bed<=MAX_FILL;
    }
}
