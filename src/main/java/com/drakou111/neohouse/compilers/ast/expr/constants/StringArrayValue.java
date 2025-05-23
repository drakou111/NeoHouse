package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class StringArrayValue implements ConstantArray {
    public final List<String> value;
    public StringArrayValue(List<String> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }
}
