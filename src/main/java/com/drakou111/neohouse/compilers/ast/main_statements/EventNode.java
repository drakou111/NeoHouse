package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.Event;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class EventNode extends MainStatementNode {
    public final Event event;
    public final StatementListNode statements;

    public EventNode(final Event event, final StatementListNode statements, Token token) {
        this.event = event;
        this.statements = statements;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitEvent(this);
    }

    @Override
    public String toString() {
        return "EventNode[event=" + event + ", statements=" + statements + "]";
    }

    @Override
    public String toShortString() {
        return "Event[e=" + event + ", stmts=" + statements.toShortString() + "]";
    }
}
