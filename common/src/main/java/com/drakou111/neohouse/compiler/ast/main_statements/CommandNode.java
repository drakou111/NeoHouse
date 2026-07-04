package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class CommandNode extends MainStatementNode {
    public final String name;
    public final boolean targeted;
    public final int priority;
    public final boolean listed;
    public final StatementListNode statements;
    public final boolean isConst;

    public CommandNode(String name, boolean targeted, int priority, boolean listed, StatementListNode statements, boolean isConst, Token token) {
        this.name = name;
        this.targeted = targeted;
        this.priority = priority;
        this.listed = listed;
        this.statements = statements;
        this.isConst = isConst;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitCommand(this);
    }

    @Override
    public String toString() {
        return "CommandNode[name=" + name + ", targeted=" + targeted + ", priority=" + priority + ", listed=" + listed + "]";
    }

    @Override
    public String toShortString() {
        return "Command[" + name + "]";
    }
}
