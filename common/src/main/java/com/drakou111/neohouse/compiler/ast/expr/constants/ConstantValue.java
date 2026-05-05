package com.drakou111.neohouse.compiler.ast.expr.constants;

public sealed interface ConstantValue permits ConstantPrimitive, ConstantArray {
    Object getValue();
    String toString();
    String toShortString();
}
