package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class DoubleArrayValue implements ConstantArray {
    public final List<Double> value;
    public DoubleArrayValue(final List<Double> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }
}
