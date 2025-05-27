package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class VoidFunctionCallNode extends ExpressionNode {
    public final String functionName;
    public final boolean global;

    public VoidFunctionCallNode(final String functionName, final boolean global, final Token token) {
        this.functionName = functionName;
        this.global = global;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidFunctionCallStatement(this);
    }

    @Override
    public String toString() {
        return "VoidFunctionCallNode [functionName=" + functionName + ", global=" + global + "]";
    }

    @Override
    public String toShortString() {
        return "VoidFunCall[name=" + functionName + ", global=" + global + "]";
    }
}
