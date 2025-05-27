package com.drakou111.neohouse.compilers.ast.expr.var;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.Holder;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class VariableNode extends ExpressionNode {
    public final VariableType type;
    public final Holder holder;
    public final String name;

    public VariableNode(final VariableType type, final Holder holder, final String name, final Token token) {
        this.type = type;
        this.holder = holder;
        this.name = name;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariable(this);
    }

    @Override
    public String toString() {
        return "VariableNode [type=" + type + ", holder=" + holder + ", name=" + name + "]";
    }

    @Override
    public String toShortString() {
        return "Var[type=" + type + ", hold=" + holder + ", name=" + name + "]";
    }
}
