package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class LongArrayValue implements ConstantArray {
    public final List<Long> value;
    public LongArrayValue(List<Long> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }
}
