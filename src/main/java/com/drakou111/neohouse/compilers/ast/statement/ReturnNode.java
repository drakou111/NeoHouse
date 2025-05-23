package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;

public class ReturnNode extends StatementNode {
    public final ExpressionNode expression;

    public ReturnNode(final ExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReturn(this);
    }
}
