package com.drakou111.neohouse.compilers.ast.event;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;

public class EventNode extends MainStatementNode {
    public final String name;
    public final StatementListNode statements;

    public EventNode(final String name, final StatementListNode statements) {
        this.name = name;
        this.statements = statements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitEvent(this);
    }
}
