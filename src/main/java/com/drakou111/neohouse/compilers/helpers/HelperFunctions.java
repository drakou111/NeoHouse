package com.drakou111.neohouse.compilers.helpers;

import java.util.Map;

public class HelperFunctions {
    public Map<String, String> ALL_FUNCTIONS = Map.of(
        "sin", SIN,
        "cos", COS,
        "tan", TAN,
        "arcsin", ARCSIN,
        "arccos", ARCCOS,
        "arctan", ARCTAN,
        "abs", ABS,
        "random", RANDOM
    );

    public static String SIN = """
                /// NeoHouse.sin
                function #sin (double p.val) {
                }
            """;

    public static String COS = """
                /// NeoHouse.cos
                function #cos (double p.val) {
                }
            """;

    public static String TAN = """
                /// NeoHouse.tan
                function #tan (double p.val) {
                }
            """;

    public static String ARCSIN = """
                /// NeoHouse.arcsin
                function #arcsin (double p.val) {
                }
            """;

    public static String ARCCOS = """
                /// NeoHouse.arccos
                function #arccos (double p.val) {
                }
            """;

    public static String ARCTAN = """
                /// NeoHouse.arctan
                function #arctan (double p.val) {
                }
            """;

    public static String ABS = """
                /// NeoHouse.abs
                function #abs (long p.val) {
                    if (p.val < 0) {
                        return -p.val;
                    }
                    return p.val;
                }
                function #abs (double p.val) {
                    if (p.val < 0.0) {
                        return -p.val;
                    }
                    return p.val;
                }
            """;

    public static String RANDOM = """
                /// NeoHouse.random
                function #random (double p.lower, double p.upper) {
                }
            """;
}
