package com.drakou111.neohouse.compiler.ast.main;

import com.drakou111.neohouse.compiler.helpers.StringHelper;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class StatementListNode extends StatementNode {
    public final List<StatementNode> statements;

    public StatementListNode(final List<StatementNode> statements) {
        this.statements = new ArrayList<>();
        if (statements == null) return;

        for (StatementNode statement : statements) {
            if (statement instanceof StatementListNode s)
                this.statements.addAll(s.statements);
            else
                this.statements.add(statement);
        }
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStatements(this);
    }

    @Override
    public String toString() {
        return "StatementListNode[statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "StmtList[stmts=" + StringHelper.nodeListToString(statements) + "]";
    }
}
