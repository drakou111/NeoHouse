package com.drakou111.neohouse.compilers.ast.enums;

public enum HolderType {
    P, G, T;

    public static HolderType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "p" -> P;
            case "g" -> G;
            case "t" -> T;
            default -> throw new IllegalArgumentException("Unknown holder: " + s);
        };
    }
}
