package com.drakou111.neohouse.compiler.ast.expr.constants;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public class ConstantArrayNode extends NormalExpressionNode {
    public final ConstantArray value;

    public ConstantArrayNode(final ConstantArray value, VariableType type, final Token token) {
        super(type);
        this.value = value;
        this.token = token;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstantArray(this);
    }

    @Override
    public String toString() {
        return "ConstantArrayNode[value=" + value + ", type=" + type + "]";
    }

    @Override
    public String toShortString() {
        return "ConstArr[v=" + value.toShortString() + ", type=" + type + "]";
    }
}