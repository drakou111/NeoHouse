package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.constants.Event;

public class EventNode extends MainStatementNode {
    public final Event event;
    public final StatementListNode statements;
    public final boolean isConst;

    public EventNode(final Event event, final StatementListNode statements, final boolean isConst, Token token) {
        this.event = event;
        this.statements = statements;
        this.isConst = isConst;
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
