package com.drakou111.neohouse.compilers.ast.enums;

public enum BooleanOperator {
    EQUALS,                 // int == int [or] double == double [or] bool == bool [or] string == string
    NOT_EQUALS,             // int != int [or] double != double [or] bool != bool [or] string != string
    LESS_THAN,              // int < int [or] double < double
    GREATER_THAN,           // int > int [or] double > double
    LESS_THAN_EQUALS,       // int <= int [or] double <= double
    GREATER_THAN_EQUALS,    // int >= int [or] double >= double
}
