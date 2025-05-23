package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;

public class GlobalFunctionCallNode extends FunctionCallNode {
    public GlobalFunctionCallNode(final String functionName) {
        super(functionName);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalFunctionCall(this);
    }
}
