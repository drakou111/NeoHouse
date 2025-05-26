package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.BinaryOperator;

public class BinaryExpressionNode extends ExpressionNode {
    public final BinaryOperator operator;
    public final ExpressionNode left;
    public final ExpressionNode right;

    public BinaryExpressionNode(final ExpressionNode left, final ExpressionNode right, final BinaryOperator operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }

    @Override
    public String toString() {
        return "BinaryExpressionNode [operator=" + operator + ", left=" + left + ", right=" + right + "]";
    }

    @Override
    public String toShortString() {
        return "BExpr[op=" + operator + ", l=" + left.toShortString() + ", r=" + right.toShortString() + "]";
    }
}
