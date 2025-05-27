package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class ConstantPrimitiveNode extends ExpressionNode {
    public final ConstantPrimitive value;

    public ConstantPrimitiveNode(final ConstantPrimitive value, final Token token) {
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantPrimitive(this);
    }

    @Override
    public String toString() {
        return "ConstantPrimitiveNode [value=" + value + "]";
    }

    @Override
    public String toShortString() {
        return "ConstPrim[v=" + value.toShortString() + "]";
    }
}