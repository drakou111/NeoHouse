package com.drakou111.neohouse.config;

public enum RecursionMode {
    OFF("Off"),
    WARN("Warn"),
    ERROR("Error");

    public final String displayName;

    RecursionMode(String displayName) {
        this.displayName = displayName;
    }

    public static RecursionMode byId(String id) {
        for (RecursionMode m : values()) if (m.name().equals(id)) return m;
        return WARN;
    }
}
