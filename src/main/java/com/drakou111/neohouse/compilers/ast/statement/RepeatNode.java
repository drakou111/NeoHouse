package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class RepeatNode extends StatementNode {
    public final long count;
    public final StatementListNode statements;

    public RepeatNode(final long count, final StatementListNode statements, final Token token) {
        this.count = count;
        this.statements = statements;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitRepeat(this);
    }

    @Override
    public String toString() {
        return "RepeatNode[count=" + count + ", statements=" + statements + "]";
    }
    @Override
    public String toShortString() {
        return "Repeat[c=" + count + ", stmts=" + statements.toShortString() + "]";
    }
}
