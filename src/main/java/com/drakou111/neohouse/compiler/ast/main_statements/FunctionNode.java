package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.exceptions.SemanticException;

public abstract class FunctionNode extends MainStatementNode {
    public final String name;
    public final String description;
    public StatementListNode statements;

    public FunctionNode(final String name, final String description, final StatementListNode statements, final Token token) {
        this.name = name;
        this.description = description;
        this.statements = statements;
        this.token = token;

        if (name.length() > 50)
            throw new SemanticException(token, "The function named '" + name + "' is too long.");
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
