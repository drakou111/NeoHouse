package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

public class ConstantNode extends ExpressionNode {
    public final ConstantValue value;

    public ConstantNode(final ConstantValue value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstant(this);
    }

    @Override
    public String toString() {
        return "ConstantNode [value=" + value + "]";
    }

    @Override
    public String toShortString() {
        return "Const[v=" + value.toShortString() + "]";
    }
}