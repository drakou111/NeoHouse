package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.UnaryOperator;
import com.drakou111.neohouse.compilers.lexer.Token;

public class UnaryExpressionNode extends ExpressionNode {
    public final UnaryOperator operator;
    public final ExpressionNode operand;

    public UnaryExpressionNode(final UnaryOperator operator, final ExpressionNode operand, final Token token) {
        this.operator = operator;
        this.operand = operand;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }

    @Override
    public String toString() {
        return "UnaryExpressionNode[operator=" + operator + ", operand=" + operand + "]";
    }

    @Override
    public String toShortString() {
        return "UExpr[op=" + operator + ", e=" + operand.toShortString() + "]";
    }
}
