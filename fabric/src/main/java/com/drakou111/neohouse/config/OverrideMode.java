package com.drakou111.neohouse.config;

public enum OverrideMode {
    ALWAYS("Always Override"),
    NEVER("Never Override"),
    IF_EMPTY("Override If Empty"),
    APPEND("Append");

    public final String displayName;

    OverrideMode(String displayName) {
        this.displayName = displayName;
    }

    public static OverrideMode byId(String id) {
        for (OverrideMode m : values()) if (m.name().equals(id)) return m;
        return IF_EMPTY;
    }
}
