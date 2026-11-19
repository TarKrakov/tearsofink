package com.tarkrakov.tearsofink.entity;

/** D-055: pilot balance; speeds in blocks/tick, intervals in ticks. */
public final class PaleOwlTuning {
    private PaleOwlTuning() {}
    public static final double HEALTH = 10.0;
    public static final int SPAWN_WEIGHT = 4, GROUP_MIN = 1, GROUP_MAX = 3;
    // Local replenishment on explored terrain, separate from vanilla CREATURE capacity.
    public static final int ENCOUNTER_INTERVAL = 200, ENCOUNTER_COLUMNS = 20;
    public static final int ENCOUNTER_TARGET = 3, ENCOUNTER_NEAR_RADIUS = 64;
    public static final int ENCOUNTER_CAP = 4, ENCOUNTER_CAP_RADIUS = 96;
    public static final int ENCOUNTER_MIN_DISTANCE = 24, ENCOUNTER_MAX_DISTANCE = 44;
    public static final int SWAY_MIN = 160, SWAY_SPREAD = 240;
    public static final int REST_MIN = 400, REST_SPREAD = 600;
    public static final int FLIGHT_MIN = 100, FLIGHT_SPREAD = 140;
    public static final double PERCH_WING_CLEARANCE = .40;
    public static final double FLIGHT_SPEED = 0.23, DIVE_SPEED = 0.34, LAND_SPEED = 0.13;
    // D-074: hungry on arrival; successful strikes are spaced, failed dives retry sooner.
    public static final int HUNT_FIRST_MIN = 100, HUNT_FIRST_SPREAD = 100;
    public static final int HUNT_MIN = 600, HUNT_SPREAD = 400;
    public static final int HUNT_RETRY_MIN = 100, HUNT_RETRY_SPREAD = 80, SEARCH_INTERVAL = 40;
    public static final int HUNT_CANDIDATES = 3, HUNT_LAUNCH_LIMIT = 60, HUNT_LIMIT = 160, HUNT_RETURN_LIMIT = 240;
    public static final double HUNT_RADIUS = 16.0, HUNT_VERTICAL_RANGE = 24.0;
    public static final float HUNT_DAMAGE = 3.0f;
    public static final int TAKEOFF_TICKS = 26, SWAY_TICKS = 60, LAND_TICKS = 20;
    // Audio only; no gameplay tuning changes.
    public static final int HOOT_FIRST_MIN = 40, HOOT_FIRST_SPREAD = 60;
    public static final int HOOT_MIN = 160, HOOT_SPREAD = 120;
    public static final float HOOT_VOLUME = 0.24f;
    public static final float TAKEOFF_WING_VOLUME = 0.34f, FLY_WING_VOLUME = 0.25f, LAND_WING_VOLUME = 0.28f;
}
