package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.ast.main.ArgumentListNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class ConditionCallNode extends ConditionExpressionNode {
    public final String name;
    public final ArgumentListNode arguments;

    public ConditionCallNode(String name, ArgumentListNode arguments, Token token) {
        super(VariableType.BOOL);
        this.name = name;
        this.arguments = arguments;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConditionCall(this);
    }

    @Override
    public String toString() {
        return "ConditionCallNode [name=" + name + ", arguments=" + arguments + "]";
    }

    @Override
    public String toShortString() {
        return "CondCall [name=" + name + ", args=" + arguments.toShortString() + "]";
    }
}
