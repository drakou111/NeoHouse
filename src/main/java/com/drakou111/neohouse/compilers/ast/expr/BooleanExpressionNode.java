package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.BooleanOperator;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.lexer.Token;

public class BooleanExpressionNode extends ConditionExpressionNode {
    public final BooleanOperator operator;
    public final NormalExpressionNode left;
    public final NormalExpressionNode right;

    public BooleanExpressionNode(final NormalExpressionNode left, final NormalExpressionNode right, final BooleanOperator operator, final Token token) {
        super(VariableType.BOOL);
        this.left = left;
        this.right = right;
        this.operator = operator;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBooleanExpr(this);
    }

    @Override
    public String toString() {
        return "BooleanExpressionNode[operator=" + operator + ", type=" + type + ", left=" + left + ", right=" + right + "]";
    }

    @Override
    public String toShortString() {
        return "BExpr[op=" + operator + ", type=" + type + ", l=" + left.toShortString() + ", r=" + right.toShortString() + "]";
    }
}
