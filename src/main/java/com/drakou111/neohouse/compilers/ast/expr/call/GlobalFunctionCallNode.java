package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.lexer.Token;

public class GlobalFunctionCallNode extends FunctionCallNode {

    public GlobalFunctionCallNode(final String functionName, final Token token) {
        super(functionName, token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalFunctionCall(this);
    }

    @Override
    public String toString() {
        return "GlobalFunctionCallNode [functionName=" + functionName + "]";
    }

    @Override
    public String toShortString() {
        return "GFunCall[name=" + functionName + "]";
    }
}
