package com.drakou111.neohouse.compiler.ast.main;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class MainNode extends ASTNode {
    public final List<MainStatementNode> statements;

    public MainNode(final List<MainStatementNode> statements) {
        if (statements == null || statements.isEmpty())
            this.statements = new ArrayList<>();
        else
            this.statements = new ArrayList<>(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitMain(this);
    }

    @Override
    public String toString() {
        return "MainNode[statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Main[stmts=" + StringHelper.nodeListToString(statements) + "]";
    }
}
