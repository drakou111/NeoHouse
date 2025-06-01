package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.UnaryOperator;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.lexer.Token;

public class UnaryExpressionNode extends NormalExpressionNode {
    public final UnaryOperator operator;
    public final NormalExpressionNode operand;

    public UnaryExpressionNode(final UnaryOperator operator, final NormalExpressionNode operand, VariableType type, final Token token) {
        super(type);
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
        return "UnaryExpressionNode[operator=" + operator + ", type=" + type + ", operand=" + operand + "]";
    }

    @Override
    public String toShortString() {
        return "UExpr[op=" + operator + ", type=" + type + ", e=" + operand.toShortString() + "]";
    }
}
