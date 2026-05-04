package com.drakou111.neohouse.constants;

public enum Comparator {
    LESS_THAN("Less Than"),
    LESS_THAN_OR_EQUAL("Less Than or Equal"),
    EQUAL("Equal"),
    GREATER_THAN_OR_EQUAL("Greater Than or Equal"),
    GREATER_THAN("Greater Than");

    public final String displayName;

    Comparator(String displayName) {
        this.displayName = displayName;
    }
}
