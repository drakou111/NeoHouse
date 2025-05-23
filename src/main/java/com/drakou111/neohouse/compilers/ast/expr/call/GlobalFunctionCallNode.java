package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

public class GlobalFunctionCallNode extends FunctionCallNode {
    public GlobalFunctionCallNode(String functionName) {
        super(functionName);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalFunctionCall(this);
    }
}
