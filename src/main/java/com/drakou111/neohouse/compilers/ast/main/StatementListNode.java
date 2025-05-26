package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class StatementListNode extends ASTNode {
    public final List<StatementNode> statements;

    public StatementListNode(final List<StatementNode> statements) {
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStatements(this);
    }

    @Override
    public String toString() {
        return "StatementListNode [statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "StmtList[stmts=" + StringHelper.nodeListToString(statements) + "]";
    }
}
