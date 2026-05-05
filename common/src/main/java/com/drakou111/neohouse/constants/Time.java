package com.drakou111.neohouse.constants;

public enum Time {
    RESET_WORLD_TIME("Reset to World Time"),
    SUNRISE("Sunrise (0)"),
    NOON("Noon (6,000)"),
    SUNSET("Sunset (12,000)"),
    MIDNIGHT("Midnight (18,000)"),
    CUSTOM("Custom Time");

    public final String displayName;

    Time(String displayName) {
        this.displayName = displayName;
    }
}
