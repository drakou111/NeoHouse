package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

public final class StringValue implements ConstantPrimitive {
    public final String value;
    public StringValue(final String value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }

    @Override
    public String toShortString() {
        return toString();
    }

    @Override
    public VariableType getType() {
        return VariableType.STRING;
    }
}