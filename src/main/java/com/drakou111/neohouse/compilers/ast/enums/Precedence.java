package com.drakou111.neohouse.compilers.ast.enums;

public enum Precedence {
    NONE,
    TERM,       // + -
    FACTOR,     // * /
    UNARY,      // ! -
    POSTFIX,    // ++ --
    CALL        // function call
}
