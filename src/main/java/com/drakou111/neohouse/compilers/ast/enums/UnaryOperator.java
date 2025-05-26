package com.drakou111.neohouse.compilers.ast.enums;

public enum UnaryOperator {
    NEGATE,                 // -int [or] -double
    NOT,                    // !bool
    INC,                    // int++ [or] double++
    DEC,                    // int-- [or] double--
    LEN                     // arr.length
}
