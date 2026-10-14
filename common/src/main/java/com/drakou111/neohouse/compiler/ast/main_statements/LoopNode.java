package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class LoopNode extends FunctionNode {
    public final long tickDelay;

    public LoopNode(final String name, final String description, final String icon, final StatementListNode statements, final long delay, final Token token) {
        super(name, description, icon, statements, token);
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
