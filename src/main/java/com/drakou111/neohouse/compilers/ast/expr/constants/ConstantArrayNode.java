package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

public class ConstantArrayNode extends ExpressionNode {
    public final ConstantArray value;

    public ConstantArrayNode(final ConstantArray value, final Token token) {
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantArray(this);
    }

    @Override
    public String toString() {
        return "ConstantArrayNode [value=" + value + "]";
    }

    @Override
    public String toShortString() {
        return "ConstArr[v=" + value.toShortString() + "]";
    }
}