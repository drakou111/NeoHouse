package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

public abstract class FunctionCallNode extends ExpressionNode {
    public final String functionName;

    public FunctionCallNode(String functionName) {
        this.functionName = functionName;
    }
}
