package com.drakou111.neohouse.compiler.enums;

public enum BinaryOperator {
    ADD,                    // int + int [or] double + double [or] string + string
    SUBTRACT,               // int - int [or] double - double
    MULTIPLY,               // int * int [or] double * double
    DIVIDE,                 // int / int [or] double / double
    BITWISE_OR,             // |
    BITWISE_AND,            // &
    BITWISE_XOR,            // ^
    LEFT_SHIFT,             // <<
    RIGHT_SHIFT,            // >>
    UNSIGNED_RIGHT_SHIFT,   // >>>
}
