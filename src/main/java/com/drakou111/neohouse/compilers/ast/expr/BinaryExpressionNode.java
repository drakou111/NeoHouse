package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.Operator;

public class BinaryExpressionNode extends ExpressionNode {
    public final Operator operator;
    public final ExpressionNode left;
    public final ExpressionNode right;

    public BinaryExpressionNode(ExpressionNode left, ExpressionNode right, Operator operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
