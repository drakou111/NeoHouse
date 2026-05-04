package com.drakou111.neohouse.constants;

public enum Operator {
    SET("Set", false),
    UNSET("Unset", false),
    INCREMENT("Increment", false),
    DECREMENT("Decrement", false),
    MULTIPLY("Multiply", false),
    DIVIDE("Divide", false),
    BITWISE_AND("Bitwise AND", true),
    BITWISE_OR("Bitwise OR", true),
    BITWISE_XOR("Bitwise XOR", true),
    LEFT_SHIFT("Left Shift", true),
    ARITHMETIC_RIGHT_SHIFT("Arithmetic Right Shift", true),
    LOGICAL_RIGHT_SHIFT("Logical Right Shift", true),
    ADVANCED_OPERATIONS("Toggle Advanced Operations", false);

    public final String displayName;
    public final boolean requiresToggle;

    Operator(String displayName, boolean requiresToggle) {
        this.displayName = displayName;
        this.requiresToggle = requiresToggle;
    }
}
