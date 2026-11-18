package com.tarkrakov.tearsofink.entity;

/** D-055: pilot balance; speeds in blocks/tick, intervals in ticks. */
public final class PaleOwlTuning {
    private PaleOwlTuning() {}
    public static final double HEALTH = 10.0;
    public static final int SPAWN_WEIGHT = 4, GROUP_MIN = 1, GROUP_MAX = 3;
    public static final int SWAY_MIN = 160, SWAY_SPREAD = 240;
    public static final int REST_MIN = 400, REST_SPREAD = 600;
    public static final int FLIGHT_MIN = 100, FLIGHT_SPREAD = 140;
    public static final double FLIGHT_SPEED = 0.23, DIVE_SPEED = 0.34, LAND_SPEED = 0.13;
    public static final int HUNT_MIN = 1200, HUNT_SPREAD = 1200, SEARCH_INTERVAL = 20;
    public static final double HUNT_RADIUS = 12.0;
    public static final float HUNT_DAMAGE = 3.0f;
    public static final int DIVE_LIMIT = 80;
    public static final int TAKEOFF_TICKS = 26, SWAY_TICKS = 60, LAND_TICKS = 20;
}
