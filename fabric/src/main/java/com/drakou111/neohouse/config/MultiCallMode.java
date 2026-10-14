package com.drakou111.neohouse.config;

public enum MultiCallMode {
    OFF("Off"),
    WARN("Warn"),
    ERROR("Error"),
    FIX("Fix (auto-copy)");

    public final String displayName;

    MultiCallMode(String displayName) {
        this.displayName = displayName;
    }

    public static MultiCallMode byId(String id) {
        for (MultiCallMode m : values()) if (m.name().equals(id)) return m;
        return ERROR;
    }
}
