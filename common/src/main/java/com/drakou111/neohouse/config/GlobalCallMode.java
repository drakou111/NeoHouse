package com.drakou111.neohouse.config;

public enum GlobalCallMode {
    OFF("Off"),
    WARN("Warn"),
    ERROR("Error");

    public final String displayName;

    GlobalCallMode(String displayName) {
        this.displayName = displayName;
    }

    public static GlobalCallMode byId(String id) {
        for (GlobalCallMode m : values()) if (m.name().equals(id)) return m;
        return ERROR;
    }
}
