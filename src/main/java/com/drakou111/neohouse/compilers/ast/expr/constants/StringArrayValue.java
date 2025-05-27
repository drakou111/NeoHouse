package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class StringArrayValue implements ConstantArray {
    public final List<String> value;
    public StringArrayValue(final List<String> value) { this.value = List.copyOf(value); }
    public Object getValue() { return value; }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            ret.append("\"");
            ret.append(value.get(i).toString());
            ret.append("\"");
            if (i < value.size() - 1) ret.append(",");
        }
        return ret.toString();
    }

    @Override
    public String toShortString() {
        return toString();
    }
}
