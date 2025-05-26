package com.drakou111.neohouse.compilers.ast.enums;

public enum Precedence {
    NONE,
    EQUALITY,   // == !=
    COMPARISON, // < > <= >=
    TERM,       // + -
    FACTOR,     // * /
    UNARY,      // ! -
    PRIMARY;    // 1 1.0 "1.0" true
}
