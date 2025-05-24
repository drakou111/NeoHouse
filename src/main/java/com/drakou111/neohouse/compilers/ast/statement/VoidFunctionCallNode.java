package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;

public class VoidFunctionCallNode extends ExpressionNode {
    public final String functionName;
    public final boolean global;

    public VoidFunctionCallNode(final String functionName, final boolean global) {
        this.functionName = functionName;
        this.global = global;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidFunctionCallStatement(this);
    }
}
