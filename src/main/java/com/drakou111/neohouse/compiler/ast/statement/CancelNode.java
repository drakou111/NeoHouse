package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;

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
