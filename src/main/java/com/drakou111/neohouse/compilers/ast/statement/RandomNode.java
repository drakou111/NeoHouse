package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;

public class RandomNode extends StatementNode {
    public final StatementListNode statements;

    public RandomNode(final StatementListNode statements) {
        this.statements = statements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitRandom(this);
    }

    @Override
    public String toString() {
        return "RandomNode [statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Rand[stmts=" + statements.toShortString() + "]";
    }
}
