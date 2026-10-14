package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class VoidFunctionCallNode extends StatementNode {
    public final String functionName;
    public final ArgumentListNode arguments;

    public VoidFunctionCallNode(final String functionName, final ArgumentListNode arguments, final Token token) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidFunctionCall(this);
    }

    @Override
    public String toString() {
        return "VoidFunctionCallNode[functionName=" + functionName + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "VoidFunCall[name=" + functionName + ", args=" + arguments + "]";
    }
}
