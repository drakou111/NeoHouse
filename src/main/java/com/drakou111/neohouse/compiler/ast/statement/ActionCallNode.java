package com.drakou111.neohouse.compiler.ast.statement;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class ActionCallNode extends StatementNode {
    public final String name;
    public final ArgumentListNode arguments;

    public ActionCallNode(String name, ArgumentListNode arguments, Token token) {
        this.name = name;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitActionCall(this);
    }

    @Override
    public String toString() {
        return "ActionCallNode [name=" + name + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "Action[name=" + name + ", args=" + arguments.toShortString() + "]";
    }
}