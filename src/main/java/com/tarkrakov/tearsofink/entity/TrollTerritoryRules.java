package com.tarkrakov.tearsofink.entity;

/** Troll territory and strike geometry without game dependencies. */
public final class TrollTerritoryRules {
    public static final int ROAM=25, DETECT=18, DEFEND=35, CHASE=45, MIN_GAP=150;
    public static final int FAMILY_PERCENT=20, REPLENISH=30*60*20;
    public static final int BED_MIN=5, BED_MAX=9, SPAWN_MIN=32, SPAWN_MAX=72;
    private TrollTerritoryRules(){}
    public static double distanceSq(double ax,double az,double bx,double bz){
        double dx=ax-bx,dz=az-bz;return dx*dx+dz*dz;
    }
    public static boolean within(double ax,double az,double bx,double bz,int radius){
        return distanceSq(ax,az,bx,bz)<=radius*(double)radius;
    }
    public static boolean separated(double ax,double az,double bx,double bz){
        return distanceSq(ax,az,bx,bz)>=MIN_GAP*(double)MIN_GAP;
    }
    static int variation(long seed, long position, int count) {
        long mixed = (seed ^ position) * 0x9E3779B97F4A7C15L;
        return Math.floorMod(mixed ^ (mixed >>> 32), count);
    }
    static boolean withinStrike(double dx, double dy, double dz, double forwardX, double forwardZ,
                                double range, double height, boolean area) {
        double horizontal = dx * dx + dz * dz;
        return horizontal <= range * range && Math.abs(dy) <= height
                && (area || horizontal < .0001 || dx * forwardX + dz * forwardZ >= .55 * Math.sqrt(horizontal));
    }
    public static int size(int roll){return roll<FAMILY_PERCENT?2:1;}
}
