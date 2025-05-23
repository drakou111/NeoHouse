package com.drakou111.neohouse.compilers.ast.expr.constants;

public sealed interface ConstantArray extends ConstantValue permits BoolArrayValue, DoubleArrayValue, LongArrayValue, StringArrayValue {
    Object getValue();
}
