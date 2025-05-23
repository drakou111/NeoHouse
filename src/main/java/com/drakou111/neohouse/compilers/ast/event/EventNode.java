package com.drakou111.neohouse.compilers.ast.event;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.statement.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.statement.StatementNode;

import java.util.ArrayList;
import java.util.List;

public class EventNode extends MainStatementNode {
    public final String name;
    public final List<StatementNode> statements;

    public EventNode(final String name, final List<StatementNode> statements) {
        this.name = name;
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitEvent(this);
    }
}
