package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.lexer.Token;

public class ConstantPrimitiveNode extends NormalExpressionNode {
    public final ConstantPrimitive value;

    public ConstantPrimitiveNode(final ConstantPrimitive value, VariableType type, final Token token) {
        super(type);
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantPrimitive(this);
    }

    @Override
    public String toString() {
        return "ConstantPrimitiveNode[value=" + value + ", type=" + type + "]";
    }

    @Override
    public String toShortString() {
        return "ConstPrim[v=" + value.toShortString() + ", type=" + type + "]";
    }
}