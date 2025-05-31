package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.BinaryOperator;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.lexer.Token;

public class BinaryExpressionNode extends NormalExpressionNode {
    public final BinaryOperator operator;
    public final NormalExpressionNode left;
    public final NormalExpressionNode right;

    public BinaryExpressionNode(final NormalExpressionNode left, final NormalExpressionNode right, final BinaryOperator operator, VariableType type, final Token token) {
        super(type);
        this.left = left;
        this.right = right;
        this.operator = operator;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }

    @Override
    public String toString() {
        return "BinaryExpressionNode[operator=" + operator + ", type=" + type + ", left=" + left + ", right=" + right + "]";
    }

    @Override
    public String toShortString() {
        return "BExpr[op=" + operator + ", type=" + type + ", l=" + left.toShortString() + ", r=" + right.toShortString() + "]";
    }
}
