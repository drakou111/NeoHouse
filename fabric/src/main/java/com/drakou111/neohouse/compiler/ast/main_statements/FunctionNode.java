package com.drakou111.neohouse.compiler.ast.main_statements;

import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.exceptions.SemanticException;

public abstract class FunctionNode extends MainStatementNode {
    /** Housing's limit on function names. */
    public static final int MAX_NAME_LENGTH = 50;

    public final String name;
    public final String description;
    public final String icon; // nullable SNBT string
    public final StatementListNode statements;

    public FunctionNode(final String name, final String description, final String icon, final StatementListNode statements, final Token token) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.statements = statements;
        this.token = token;

        if (name.length() > MAX_NAME_LENGTH)
            throw new SemanticException(token, "The function named '" + name + "' is too long.");
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
