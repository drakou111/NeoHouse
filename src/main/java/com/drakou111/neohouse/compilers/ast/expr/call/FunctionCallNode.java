package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public abstract class FunctionCallNode extends ExpressionNode {
    public final String functionName;

    public FunctionCallNode(final String functionName, final Token token) {
        this.functionName = functionName;
        this.token = token;
    }
}
