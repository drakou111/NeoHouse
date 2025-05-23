package com.drakou111.neohouse.compilers.ast.expr.constants;

public final class LongValue implements ConstantPrimitive {
    public final long value;
    public LongValue(long value) { this.value = value; }
    public Object getValue() { return value; }
}
