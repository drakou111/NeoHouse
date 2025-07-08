package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.BooleanOperator;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;

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
