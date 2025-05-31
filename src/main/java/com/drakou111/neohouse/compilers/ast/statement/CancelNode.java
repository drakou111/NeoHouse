package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class CancelNode extends StatementNode {

    public CancelNode(final Token token) {
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitCancel(this);
    }

    @Override
    public String toString() {
        return "CancelNode";
    }

    @Override
    public String toShortString() {
        return "Cancel";
    }
}
