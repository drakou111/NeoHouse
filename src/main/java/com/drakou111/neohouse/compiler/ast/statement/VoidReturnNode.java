package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.lexer.Token;

public class VoidReturnNode extends ReturnNode {
    public VoidReturnNode(final Token token) {
        super(token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidReturn(this);
    }

    @Override
    public String toString() {
        return "VoidReturnNode";
    }

    @Override
    public String toShortString() {
        return "VoidRet";
    }
}
