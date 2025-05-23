package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.ArgumentListNode;

public class SingleFunctionCallNode extends FunctionCallNode {
    public final ArgumentListNode arguments;

    public SingleFunctionCallNode(final String functionName, final ArgumentListNode arguments) {
        super(functionName);
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSingleFunctionCall(this);
    }
}
