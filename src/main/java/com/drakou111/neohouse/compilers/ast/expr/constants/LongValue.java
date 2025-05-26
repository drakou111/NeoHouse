package com.drakou111.neohouse.compilers.ast.expr.constants;

public final class LongValue implements ConstantPrimitive {
    public final long value;
    public LongValue(final long value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public String toShortString() {
        return Long.toString(value);
    }
}
