package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class BoolArrayValue implements ConstantArray {
    public final List<BoolValue> value;
    public BoolArrayValue(final List<BoolValue> value) { this.value = List.copyOf(value); }
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
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            ret.append(value.get(i).toShortString());
            if (i < value.size() - 1) ret.append(",");
        }
        return ret.toString();
    }
}
