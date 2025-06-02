package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public abstract class ReturnNode extends StatementNode {

    public ReturnNode(final Token token) {
        this.token = token;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
