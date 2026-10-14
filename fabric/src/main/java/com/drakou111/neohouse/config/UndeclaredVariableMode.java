package com.drakou111.neohouse.config;

/** What the compiler does with a variable used without a declaration (it becomes an untyped var). */
public enum UndeclaredVariableMode {
    ALLOW("Allow"),
    WARN("Warn"),
    ERROR("Error");

    public final String displayName;

    UndeclaredVariableMode(String displayName) {
        this.displayName = displayName;
    }

    public static UndeclaredVariableMode byId(String id) {
        for (UndeclaredVariableMode m : values()) if (m.name().equals(id)) return m;
        return ALLOW;
    }
}
