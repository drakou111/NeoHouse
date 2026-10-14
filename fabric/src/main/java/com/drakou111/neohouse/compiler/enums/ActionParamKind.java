package com.drakou111.neohouse.compiler.enums;

public enum ActionParamKind {
    STRING_LITERAL,
    BOOL_LITERAL,
    LONG_LITERAL,
    DOUBLE_LITERAL,
    NORMAL_EXPRESSION,
    ITEM_LITERAL,
    /** Accepts a bare Teleport enum name (HOUSE_SPAWN / INVOKER / CURRENT) or any string expression for custom coordinates. */
    LOCATION
}
