package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.ast.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class ConstantNode extends NormalExpressionNode {
    public final ConstantValue value;

    public ConstantNode(final ConstantValue value, VariableType type, final Token token) {
        super(type);
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstant(this);
    }

    @Override
    public String toString() {
        return "ConstantNode[value=" + value + ", type=" + type + "]";
    }

    @Override
    public String toShortString() {
        return "Const[v=" + value.toShortString() + ", type=" + type + "]";
    }
}