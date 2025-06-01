package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compilers.ast.main.StatementNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class VoidFunctionCallNode extends StatementNode {
    public final String functionName;
    public ArgumentListNode arguments;

    public VoidFunctionCallNode(final String functionName, final ArgumentListNode arguments, final Token token) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVoidFunctionCallStatement(this);
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
