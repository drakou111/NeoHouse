package com.drakou111.neohouse.config;

/** What the compiler does with a condition on a variable the script never sets (always false while unset). */
public enum UnsetVariableMode {
    OFF("Off"),
    WARN("Warn"),
    ERROR("Error");

    public final String displayName;

    UnsetVariableMode(String displayName) {
        this.displayName = displayName;
    }

    public static UnsetVariableMode byId(String id) {
        for (UnsetVariableMode m : values()) if (m.name().equals(id)) return m;
        return WARN;
    }
}
