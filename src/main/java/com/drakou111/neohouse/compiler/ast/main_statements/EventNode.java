package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;

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
