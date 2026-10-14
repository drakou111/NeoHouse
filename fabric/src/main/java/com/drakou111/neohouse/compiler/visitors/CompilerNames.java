package com.drakou111.neohouse.compiler.visitors;

import java.nio.charset.StandardCharsets;

/**
 * Names for the variables the compiler makes for a function: its expression temps and its
 * parameter slots. They depend only on that function's own name, so changing one function never
 * renames anything in another. Housing allows {@value #MAX_VARIABLE_NAME} characters, so a name
 * that wouldn't fit uses a fixed 6-character code of the function name instead
 * ({@code calculateTotal_p$0} becomes {@code $k3f9a2p0}).
 */
public final class CompilerNames {
    public static final int MAX_VARIABLE_NAME = 16;
    private static final int CODE_LENGTH = 6;
    private static final long CODE_SPACE = 2_176_782_336L; // 36^6
    private static final int RADIX = 36;
    private static final int FNV_OFFSET = 0x811c9dc5;
    private static final int FNV_PRIME = 0x01000193;

    private CompilerNames() {
    }

    /** Temp number {@code n} of the function {@code function} (copies like {@code f$1} share {@code f}'s). */
    public static String temp(String function, int n) {
        String base = baseName(function);
        String readable = base + "$t" + n;
        return readable.length() <= MAX_VARIABLE_NAME ? readable : "$" + code(base) + "t" + n;
    }

    /** Where callers leave argument {@code index} of {@code function}. */
    public static String parameterSlot(String function, int index) {
        String base = baseName(function);
        String readable = base + "_p$" + index;
        return readable.length() <= MAX_VARIABLE_NAME ? readable : "$" + code(base) + "p" + index;
    }

    /** Whether {@link #temp} or {@link #parameterSlot} would use the short code for this function. */
    public static boolean usesCode(String function) {
        String base = baseName(function);
        return (base + "_p$0").length() > MAX_VARIABLE_NAME || (base + "$t0").length() > MAX_VARIABLE_NAME;
    }

    /** The 6-character base-36 code of a function name (FNV-1a), stable across runs and machines. */
    public static String code(String function) {
        int hash = FNV_OFFSET;
        for (byte b : baseName(function).getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xFF;
            hash *= FNV_PRIME;
        }
        String digits = Long.toString(Integer.toUnsignedLong(hash) % CODE_SPACE, RADIX);
        return "0".repeat(CODE_LENGTH - digits.length()) + digits;
    }

    /** Whether the compiler made this name (a helper, copy or temp): scripts can't use '$' in their own names. */
    public static boolean isCompilerMade(String name) {
        return name.indexOf('$') >= 0;
    }

    /** A function copy's original name: {@code f$1} is a copy of {@code f}. */
    public static String baseName(String function) {
        int idx = function.lastIndexOf('$');
        if (idx > 0) {
            String tail = function.substring(idx + 1);
            if (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit)) return function.substring(0, idx);
        }
        return function;
    }
}
