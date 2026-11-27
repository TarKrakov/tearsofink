package com.tarkrakov.tearsofink.entity;

/** Troll territory and strike geometry without game dependencies. */
public final class TrollTerritoryRules {
    public static final int ROAM=25, DETECT=18, DEFEND=35, CHASE=45, MIN_GAP=75;
    public static final int FAMILY_PERCENT=20, REPLENISH=30*60*20;
    public static final int BED_MIN=5, BED_MAX=9, SPAWN_MIN=32, SPAWN_MAX=72;
    // Frequent bounded searches while travelling, not one random attempt per 45 s.
    public static final int SEARCH_INTERVAL=60, SEARCH_COLUMNS=8;
    public static final int MOUTH_MIN=39, MOUTH_MAX=62;
    static final int STRUCTURE_MARGIN=3;
    public static final double PLAYER_PURSUIT_SPEED=2.0, LEAP_RANGE=10.0, LEAP_RISE=8.0, LEAP_APEX=8.5;
    public static final int LEAP_WINDUP=10, LEAP_FLIGHT=32, LEAP_RECOVERY=30, NO_HIT_LIMIT=100;
    static boolean needsLeap(double distanceSquared,int noHitTicks){
        return distanceSquared>9.0||noHitTicks>=NO_HIT_LIMIT;
    }
    static double leapHorizontal(double progress){
        double t=Math.clamp((progress-.18)/.60,0,1);
        return t*t*(3-2*t);
    }
    static double leapHeight(double progress,double rise){
        double t=Math.clamp(progress,0,1);
        double end=Math.clamp(rise,-8,LEAP_RISE);
        if(t>=1)return end;
        if(t<=.42){double u=t/.42;return LEAP_APEX*(2*u-u*u);}
        double u=(t-.42)/.58;
        return LEAP_APEX+(end-LEAP_APEX)*u*u;
    }
    static final int CLEARING_RADIUS=25, CLEARING_MAX_CUT=32, CLEARING_TARGET_STUMPS=18, CLEARING_MAX_ADDED=12;
    static int clearingChance(int distanceSq) {
        return distanceSq<=14*14?100:distanceSq<=20*20?85:55;
    }
    static boolean overlapsProtectedColumn(int x,int z,int minX,int minZ,int maxX,int maxZ){
        return x>=minX-STRUCTURE_MARGIN&&x<=maxX+STRUCTURE_MARGIN
                &&z>=minZ-STRUCTURE_MARGIN&&z<=maxZ+STRUCTURE_MARGIN;
    }
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
