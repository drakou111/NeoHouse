package com.drakou111.neohouse.constants;

public enum PortalType {
    NETHER_PORTAL("Nether Portal"),
    END_PORTAL("End Portal");

    public final String displayName;

    PortalType(String displayName) {
        this.displayName = displayName;
    }
}
