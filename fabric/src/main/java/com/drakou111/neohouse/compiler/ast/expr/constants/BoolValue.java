package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.enums.VariableType;

public final class BoolValue implements ConstantPrimitive {
    public final boolean value;
    public BoolValue(final boolean value) { this.value = value; }
    public Object getValue() { return value; }

    @Override
    public VariableType getType() {
        return VariableType.BOOL;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public String toShortString() {
        return value ? "T" : "F";
    }
}