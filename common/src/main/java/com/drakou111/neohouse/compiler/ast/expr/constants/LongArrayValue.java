package com.drakou111.neohouse.compiler.ast.expr.constants;

import java.util.List;

public final class LongArrayValue implements ConstantArray {
    public final List<Long> value;
    public LongArrayValue(final List<Long> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            ret.append(value.get(i).toString());
            if (i < value.size() - 1) ret.append(",");
        }
        return ret.toString();
    }

    @Override
    public String toShortString() {
        return toString();
    }
}
