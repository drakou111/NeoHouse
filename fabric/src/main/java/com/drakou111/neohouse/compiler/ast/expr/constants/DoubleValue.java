package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

import java.math.BigDecimal;

public final class DoubleValue implements ConstantPrimitive {
    public final double value;
    public DoubleValue(final double value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public VariableType getType() {
        return VariableType.DOUBLE;
    }

    /**
     * The value written out in full (0.0001, 10000000000000000.0), never in Java's 1.0E-4 notation,
     * since the text ends up in chat messages and Housing's number fields.
     */
    @Override
    public String toString() {
        if (!Double.isFinite(this.value)) return Double.toString(this.value);
        String plain = new BigDecimal(Double.toString(this.value)).stripTrailingZeros().toPlainString();
        return plain.contains(".") ? plain : plain + ".0";
    }

    @Override
    public String toShortString() {
        return String.format("%.2f", value);
    }


}