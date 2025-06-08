package com.drakou111.neohouse.compiler.ast.expr.var;

import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class VariableNode extends NormalExpressionNode {
    public final Holder holder;
    public final String name;
    public final String teamName;

    public VariableNode(final VariableType type, final Holder holder, final String name, final String teamName, final Token token) {
        super(type);
        this.type = type;
        this.holder = holder;
        this.name = name;
        this.teamName = teamName;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariable(this);
    }

    @Override
    public String toString() {
        return "VariableNode[type=" + type + ", holder=" + holder + ", name=" + name + ", teamName=" + teamName + "]";
    }

    @Override
    public String toShortString() {
        return "Var[type=" + type + ", hold=" + holder + ", name=" + name + ", tname=" + teamName + "]";
    }
}
