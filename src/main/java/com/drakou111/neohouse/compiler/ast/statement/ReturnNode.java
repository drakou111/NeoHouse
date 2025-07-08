package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public abstract class ReturnNode extends StatementNode {

    public ReturnNode(final Token token) {
        this.token = token;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
