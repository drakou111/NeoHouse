package com.drakou111.neohouse.compilers.ast.expr.constants;

public final class BoolValue implements ConstantPrimitive {
    public final boolean value;
    public BoolValue(boolean value) { this.value = value; }
    public Object getValue() { return value; }
}