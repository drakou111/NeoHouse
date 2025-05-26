package com.drakou111.neohouse.compilers.ast.expr.constants;

public sealed interface ConstantValue permits ConstantPrimitive, ConstantArray {
    Object getValue();
    String toString();
    String toShortString();
}
