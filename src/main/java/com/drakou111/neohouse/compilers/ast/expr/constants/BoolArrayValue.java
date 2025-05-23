package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class BoolArrayValue implements ConstantArray {
    public final List<BoolValue> value;
    public BoolArrayValue(final List<BoolValue> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }
}
