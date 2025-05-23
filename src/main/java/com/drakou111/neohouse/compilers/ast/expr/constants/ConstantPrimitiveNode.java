package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

public class ConstantPrimitiveNode extends ExpressionNode {
    public final ConstantPrimitive value;

    public ConstantPrimitiveNode(final ConstantPrimitive value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantPrimitive(this);
    }
}