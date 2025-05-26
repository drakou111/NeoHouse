package com.drakou111.neohouse.compilers.ast.expr.constants;

import java.util.List;

public final class DoubleArrayValue implements ConstantArray {
    public final List<Double> value;
    public DoubleArrayValue(final List<Double> value) { this.value = List.copyOf(value); }
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
            double v = value.get(i);
            ret.append(String.format("%.2f", v));
            if (i < value.size() - 1) ret.append(",");
        }
        return ret.toString();
    }
}
