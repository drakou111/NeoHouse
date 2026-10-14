package com.drakou111.neohouse.compiler.parser;

import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.ParsingException;

import java.math.BigInteger;

/**
 * Converts number literal tokens to values. Long literals may be decimal ({@code 1_000}),
 * hexadecimal ({@code 0xFF_FF00}) or binary ({@code 0b1010}); prefixes are case-insensitive.
 * Hex and binary literals may use all 64 bits (like Java, {@code 0xFFFFFFFFFFFFFFFF} is -1).
 */
public final class NumberLiterals {
    private static final int HEX_RADIX = 16;
    private static final int BINARY_RADIX = 2;
    private static final int DECIMAL_RADIX = 10;
    private static final int PREFIX_LENGTH = 2;
    private static final int LONG_BITS = 64;

    private NumberLiterals() {}

    public static long parseLong(Token token) {
        return parseLong(token, false);
    }

    /** Parses a long literal that a unary minus directly precedes, so {@code -9223372036854775808} fits. */
    public static long parseNegatedLong(Token token) {
        return parseLong(token, true);
    }

    public static double parseDouble(Token token) {
        return Double.parseDouble(token.text.replace("_", ""));
    }

    private static long parseLong(Token token, boolean negated) {
        String text = token.text.replace("_", "");
        String lower = text.toLowerCase();
        int radix = lower.startsWith("0x") ? HEX_RADIX : lower.startsWith("0b") ? BINARY_RADIX : DECIMAL_RADIX;
        String digits = radix == DECIMAL_RADIX ? text : text.substring(PREFIX_LENGTH);

        BigInteger value = new BigInteger(digits, radix);
        if (radix != DECIMAL_RADIX) {
            if (value.bitLength() > LONG_BITS)
                throw new ParsingException(token, "'" + token.text + "' does not fit in 64 bits");
            long bits = value.longValue();
            return negated ? -bits : bits;
        }

        BigInteger signed = negated ? value.negate() : value;
        if (signed.bitLength() >= LONG_BITS)
            throw new ParsingException(token, "'" + (negated ? "-" : "") + token.text + "' is too large for a long (limit is "
                    + Long.MIN_VALUE + " to " + Long.MAX_VALUE + ")");
        return signed.longValue();
    }
}
