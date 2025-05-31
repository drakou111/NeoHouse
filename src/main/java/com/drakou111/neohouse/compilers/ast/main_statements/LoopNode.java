package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class LoopNode extends MainStatementNode {
    public final String name;
    public final String description;
    public final StatementListNode statements;
    public final long tickDelay;

    public LoopNode(final String name, final String description, final StatementListNode statements, final long delay, final Token token) {
        this.name = name;
        this.description = description;
        this.statements = statements;
        this.tickDelay = delay;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLoop(this);
    }

    @Override
    public String toString() {
        return "LoopNode[name=" + name + ", description=" + description + ", statements=" + statements + ", tickDelay=" + tickDelay + "]";
    }

    @Override
    public String toShortString() {
        return "Loop[name=" + name + ", desc=" + description + ", stmts=" + statements.toShortString() + ", delay=" + tickDelay + "]";
    }
}
