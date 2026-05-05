package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class TypedReturnNode extends ReturnNode {
    public NormalExpressionNode expression;

    public TypedReturnNode(final NormalExpressionNode expression, final Token token) {
        super(token);
        this.expression = expression;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitTypedReturn(this);
    }

    @Override
    public String toString() {
        return "TypedReturnNode[expression=" + expression + "]";
    }

    @Override
    public String toShortString() {
        return "Ret[expr=" + expression.toShortString() + "]";
    }
}
