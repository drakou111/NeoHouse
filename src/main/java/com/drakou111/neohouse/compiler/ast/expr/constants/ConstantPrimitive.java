package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

public sealed interface ConstantPrimitive extends ConstantValue permits BoolValue, DoubleValue, LongValue, StringValue{
    Object getValue();
    VariableType getType();
    String toString();
    String toShortString();
}
