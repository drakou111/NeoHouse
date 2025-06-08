package com.drakou111.neohouse.compiler.enums;

public enum UnaryOperator {
    NEGATE,                 // -int [or] -double
    NOT,                    // !bool
    INC,                    // int++ [or] double++
    DEC,                    // int-- [or] double--
    LONG_CAST,              // (long)
    DOUBLE_CAST,            // (double)
    STRING_CAST;            // (string)
}
