package com.drakou111.neohouse.config;

/** What the compiler does with a call to a function, or a displayMenu of a menu, that the script doesn't define. */
public enum UnknownCallMode {
    OFF("Off"),
    WARN("Warn"),
    ERROR("Error");

    public final String displayName;

    UnknownCallMode(String displayName) {
        this.displayName = displayName;
    }

    public static UnknownCallMode byId(String id) {
        for (UnknownCallMode m : values()) if (m.name().equals(id)) return m;
        return WARN;
    }
}
