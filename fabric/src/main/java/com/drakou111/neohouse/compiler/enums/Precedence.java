package com.drakou111.neohouse.compiler.enums;

public enum Precedence {
    NONE,
    BITWISE,    // | ^ & << >> >>>
    TERM,       // + -
    FACTOR,     // * /
    UNARY,      // ! -
    POSTFIX,    // ++ --
    CALL        // function call
}
