package com.drakou111.neohouse.compilers.ast.enums;

public enum Operator {
    ADD,                    // int + int [or] double + double [or] string + string
    SUBTRACT,               // int - int [or] double - double
    MULTIPLY,               // int * int [or] double * double
    DIVIDE,                 // int / int [or] double / double
    EQUALS,                 // int == int [or] double == double [or] bool == bool [or] string == string
    NOT_EQUALS,             // int != int [or] double != double [or] bool != bool [or] string != string
    LESS_THAN,              // int < int [or] double < double
    GREATER_THAN,           // int > int [or] double > double
    LESS_THAN_EQUALS,       // int <= int [or] double <= double
    GREATER_THAN_EQUALS,    // int >= int [or] double >= double
    AND,                    // bool && bool
    OR,                     // bool || bool
    INDEX_OF                // arr[int]
}
