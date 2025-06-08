package com.drakou111.neohouse.compiler.enums;

public enum Precedence {
    NONE,
    TERM,       // + -
    FACTOR,     // * /
    UNARY,      // ! -
    POSTFIX,    // ++ --
    CALL        // function call
}
