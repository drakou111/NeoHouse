package com.drakou111.neohouse.compiler.ast.expr.constants;

public sealed interface ConstantArray extends ConstantValue permits BoolArrayValue, DoubleArrayValue, LongArrayValue, StringArrayValue {
    Object getValue();
    String toString();
    String toShortString();
}
