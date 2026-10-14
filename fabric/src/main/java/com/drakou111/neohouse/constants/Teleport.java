package com.drakou111.neohouse.constants;

public enum Teleport {
    HOUSE_SPAWN("House Spawn Location"),
    INVOKER("Invokers Location"),
    CURRENT("Current Location"),
    CUSTOM("Custom Coordinates");

    public final String displayName;

    Teleport(String displayName) {
        this.displayName = displayName;
    }
}
