package com.drakou111.neohouse.compilers.ast.expr.call;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class FunctionCallNode extends ExpressionNode {
    public final String functionName;
    public final ArgumentListNode arguments;

    public FunctionCallNode(final String functionName, final ArgumentListNode arguments, final Token token) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSingleFunctionCall(this);
    }

    @Override
    public String toString() {
        return "FunctionCallNode[functionName=" + functionName + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "FunCall[name=" + functionName + ", args=" + arguments.toShortString() + "]";
    }
}
