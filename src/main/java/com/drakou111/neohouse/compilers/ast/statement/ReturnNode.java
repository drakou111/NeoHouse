package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class ReturnNode extends StatementNode {
    public final ExpressionNode expression;

    public ReturnNode(final ExpressionNode expression, final Token token) {
        this.expression = expression;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReturn(this);
    }

    @Override
    public String toString() {
        return "ReturnNode [expression=" + expression + "]";
    }

    @Override
    public String toShortString() {
        return "Ret[expr=" + expression.toShortString() + "]";
    }
}
