package com.drakou111.neohouse.compilers.ast.enums;

public enum Precedence {
    NONE,
    EQUALITY,   // == !=
    COMPARISON, // < > <= >=
    TERM,       // + -
    FACTOR,     // * /
    UNARY,      // ! -
    POSTFIX,    // ++ --
    CALL        // function call
}
