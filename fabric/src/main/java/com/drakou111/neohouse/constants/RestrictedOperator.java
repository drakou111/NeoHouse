package com.drakou111.neohouse.constants;

public enum RestrictedOperator {
    SET("Set"),
    INCREMENT("Increment"),
    DECREMENT("Decrement"),
    MULTIPLY("Multiply"),
    DIVIDE("Divide");

    public final String displayName;

    RestrictedOperator(String displayName) {
        this.displayName = displayName;
    }
}
