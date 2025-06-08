package com.drakou111.neohouse.compiler.ast.main;

import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.compiler.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class StatementListNode extends StatementNode {
    public List<StatementNode> statements;

    public StatementListNode(final List<StatementNode> statements) {
        if (statements == null || statements.isEmpty())
            this.statements = new ArrayList<>();
        else
            this.statements = new ArrayList<>(statements);
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
