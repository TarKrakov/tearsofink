package com.tarkrakov.tearsofink.entity;

/** D-075: provisional gameplay tuning, distances in blocks, time in server ticks. */
public final class PaleDeerTuning {
    private PaleDeerTuning() {}
    public static final double HEALTH = 16, WIDTH = 1.1, HEIGHT = 3.5;
    // Navigation multipliers on MOVEMENT_SPEED; actual ground speed depends on terrain.
    public static final double BASE_SPEED = .28, WALK = .72, RUN = 1.48;
    public static final int FOLLOW_RANGE = 24, PATH_MARGIN = 40, PATH_RETRY = 18;
    public static final int CELL = 224, OCCUPANCY_PERCENT = 80, ACTIVE_RADIUS = 192, HERD_CAP = 2;
    public static final int SPAWN_MIN = 40, SPAWN_MAX = 80, SPAWN_INTERVAL = 200, SPAWN_COLUMNS = 18;
    public static final int HERD_MIN = 3, HERD_MAX = 10, RESPAWN_MIN = 48000, RESPAWN_SPREAD = 24000;
    // Replenish surviving herds one member at a time, never a new independent herd.
    public static final int PARTIAL_RESPAWN_MIN = 24000, PARTIAL_RESPAWN_SPREAD = 24000;
    public static final int PARTIAL_CHECK_INTERVAL = 200, PARTIAL_COLUMNS = 18;
    public static final int HOME_RADIUS = 100, ROAM_MIN = 20, ROAM_MAX = 48;
    public static final int WALK_MIN = 400, WALK_SPREAD = 500, GRAZE_MIN = 600, GRAZE_SPREAD = 1200;
    public static final int REST_MIN = 300, REST_SPREAD = 400, GUARD_MIN = 400, GUARD_SPREAD = 400;
    public static final int GUARD_DISTANCE = 13, SPREAD = 12;
    public static final int SENSE_INTERVAL = 5, SUSPICION_MIN = 30, SUSPICION_SPREAD = 10;
    public static final double[] GUARD_SIGHT = {3, 5, 9, 10}, DEER_SIGHT = {2, 3, 6, 8};
    public static final double[] GUARD_HEARING = {0, 2, 7, 14}, DEER_HEARING = {0, 1, 5, 10};
    public static final int PANIC_MIN = 240, PANIC_SPREAD = 120, ESCAPE_MIN = 60, ESCAPE_MAX = 90;
    public static final int RECOVER_MIN = 160, RECOVER_SPREAD = 120;
    public static final int WATCH_START_TICKS = 15, WATCH_ROAR_TICKS = 35, WATCH_END_TICKS = 20, ROAR_TICKS = 30;
    public static final int EAT_TICKS = 140, ROAR_DELAY_MAX = 8;
}
