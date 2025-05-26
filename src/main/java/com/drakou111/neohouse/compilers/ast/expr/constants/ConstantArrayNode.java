package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

public class ConstantArrayNode extends ExpressionNode {
    public final ConstantArray value;

    public ConstantArrayNode(final ConstantArray value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantArray(this);
    }

    @Override
    public String toString() {
        return "ConstantArrayNode [value=" + value + "]";
    }

    @Override
    public String toShortString() {
        return "ConstArr[v=" + value.toShortString() + "]";
    }
}