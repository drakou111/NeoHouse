package com.drakou111.neohouse.compiler.ast.expr.call;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class FunctionCallNode extends NormalExpressionNode {
    public final String functionName;
    public final ArgumentListNode arguments;

    public FunctionCallNode(final String functionName, final ArgumentListNode arguments, VariableType type, final Token token) {
        super(type);
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunctionCall(this);
    }

    @Override
    public String toString() {
        return "FunctionCallNode[functionName=" + functionName + ", type=" + type + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "FunCall[name=" + functionName + ", type=" + type + ", args=" + arguments.toShortString() + "]";
    }
}
