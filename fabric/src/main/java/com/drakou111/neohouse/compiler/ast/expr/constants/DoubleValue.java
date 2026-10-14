package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

public final class DoubleValue implements ConstantPrimitive {
    public final double value;
    public DoubleValue(final double value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public VariableType getType() {
        return VariableType.DOUBLE;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public String toShortString() {
        return String.format("%.2f", value);
    }


}