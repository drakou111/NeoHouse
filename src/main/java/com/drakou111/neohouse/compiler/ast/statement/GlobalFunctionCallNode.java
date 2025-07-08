package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class GlobalFunctionCallNode extends StatementNode {
    public final String functionName;
    public final ArgumentListNode arguments;

    public GlobalFunctionCallNode(final String functionName, final ArgumentListNode arguments, final Token token) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGlobalFunctionCall(this);
    }

    @Override
    public String toString() {
        return "GlobalFunctionCallNode[functionName=" + functionName + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "GFunCall[name=" + functionName + ", args=" + arguments.toShortString() + "]";
    }
}
