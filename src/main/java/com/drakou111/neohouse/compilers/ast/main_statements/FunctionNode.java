package com.drakou111.neohouse.compilers.ast.main_statements;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.MainStatementNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public abstract class FunctionNode extends MainStatementNode {
    public final String name;
    public final String description;
    public final StatementListNode statements;

    public FunctionNode(final String name, final String description, final StatementListNode statements, final Token token) {
        this.name = name;
        this.description = description;
        this.statements = statements;
        this.token = token;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
