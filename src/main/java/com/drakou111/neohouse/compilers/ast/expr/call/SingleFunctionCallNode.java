package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

public class SingleFunctionCallNode extends FunctionCallNode {
    public final List<ExpressionNode> arguments;

    public SingleFunctionCallNode(String functionName, List<ExpressionNode> arguments) {
        super(functionName);
        this.arguments = new ArrayList<>(arguments);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSingleFunctionCall(this);
    }
}
