package com.drakou111.neohouse.compilers.ast.expr.constants;

public sealed interface ConstantPrimitive extends ConstantValue permits BoolValue, DoubleValue, LongValue, StringValue{
    Object getValue();
    String toString();
    String toShortString();
}
