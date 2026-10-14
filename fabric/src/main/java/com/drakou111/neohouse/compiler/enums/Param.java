package com.drakou111.neohouse.compiler.enums;

import java.util.Arrays;

/** @param allowsCustomValue an enum-backed string that also accepts any other value (the enum values are just suggestions) */
public record Param(String name, ActionParamKind kind, String[] enumValues, Long rangeMin, Long rangeMax, Double decimalMin, Double decimalMax,
                    boolean allowsCustomValue) {


    public static final Param STRING   = new Param(null, ActionParamKind.STRING_LITERAL,    null, null, null, null, null, false);
    public static final Param BOOL     = new Param(null, ActionParamKind.BOOL_LITERAL,      null, null, null, null, null, false);
    public static final Param LONG     = new Param(null, ActionParamKind.LONG_LITERAL,      null, null, null, null, null, false);
    public static final Param DOUBLE   = new Param(null, ActionParamKind.DOUBLE_LITERAL,    null, null, null, null, null, false);
    public static final Param EXPR     = new Param(null, ActionParamKind.NORMAL_EXPRESSION, null, null, null, null, null, false);
    public static final Param ITEM     = new Param(null, ActionParamKind.ITEM_LITERAL,      null, null, null, null, null, false);
    /** HOUSE_SPAWN | INVOKER | CURRENT, or any string expression for custom coordinates. */
    public static final Param LOCATION = new Param(null, ActionParamKind.LOCATION,          null, null, null, null, null, false);

    public Param withName(String n) {
        return new Param(n, kind, enumValues, rangeMin, rangeMax, decimalMin, decimalMax, allowsCustomValue);
    }

    public String typeName() {
        return switch (kind) {
            case STRING_LITERAL    -> "string";
            case BOOL_LITERAL      -> "bool";
            case LONG_LITERAL      -> "long";
            case DOUBLE_LITERAL    -> "double";
            case NORMAL_EXPRESSION -> "expr";
            case ITEM_LITERAL      -> "item";
            case LOCATION          -> "location";
        };
    }

    /** Returns "camelCaseName: type" for use in the editor parameter hint. */
    public String hintLabel() {
        return toCamelCase(name != null ? name : typeName()) + ": " + typeName();
    }

    private static String toCamelCase(String s) {
        String[] words = s.split(" +");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(words[0].charAt(0)));
        if (words[0].length() > 1) sb.append(words[0].substring(1));
        for (int i = 1; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) sb.append(words[i].substring(1));
        }
        return sb.toString();
    }

    public static Param enumOf(Class<? extends Enum<?>> cls) {
        String[] values = Arrays.stream(cls.getEnumConstants())
                                .map(e -> ((Enum<?>) e).name())
                                .toArray(String[]::new);
        return new Param(null, ActionParamKind.STRING_LITERAL, values, null, null, null, null, false);
    }

    /** Suggests the enum's names but accepts any other non-empty string too. */
    public static Param enumOrCustom(Class<? extends Enum<?>> cls) {
        Param param = enumOf(cls);
        return new Param(null, param.kind, param.enumValues, null, null, null, null, true);
    }

    public static Param longRange(long min, long max) {
        return new Param(null, ActionParamKind.LONG_LITERAL, null, min, max, null, null, false);
    }

    public static Param doubleRange(double min, double max) {
        return new Param(null, ActionParamKind.DOUBLE_LITERAL, null, null, null, min, max, false);
    }

    public boolean hasEnumConstraint()       { return enumValues != null; }
    public boolean hasLongRangeConstraint()  { return rangeMin != null; }
    public boolean hasDoubleRangeConstraint(){ return decimalMin != null; }

    public boolean isValidEnum(String v) {
        if (enumValues == null) return true;
        if (allowsCustomValue) return !v.isBlank();
        for (String s : enumValues) if (s.equals(v)) return true;
        return false;
    }

    public boolean inRange(long v) {
        return rangeMin == null || (v >= rangeMin && v <= rangeMax);
    }

    public boolean inRange(double v) {
        return decimalMin == null || (v >= decimalMin && v <= decimalMax);
    }
}
