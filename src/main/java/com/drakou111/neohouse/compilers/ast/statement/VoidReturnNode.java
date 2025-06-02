package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

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
