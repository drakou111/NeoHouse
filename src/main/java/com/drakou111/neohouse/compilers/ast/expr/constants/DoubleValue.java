package com.drakou111.neohouse.compilers.ast.expr.constants;

public final class DoubleValue implements ConstantPrimitive {
    public final double value;
    public DoubleValue(final double value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public String toShortString() {
        return String.format("%.2f", value);
    }
}