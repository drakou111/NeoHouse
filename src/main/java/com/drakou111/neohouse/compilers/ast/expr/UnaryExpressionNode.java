package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.MonoOperator;

public class UnaryExpressionNode extends ExpressionNode {
    public final MonoOperator operator;
    public final ExpressionNode operand;

    public UnaryExpressionNode(MonoOperator operator, ExpressionNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
