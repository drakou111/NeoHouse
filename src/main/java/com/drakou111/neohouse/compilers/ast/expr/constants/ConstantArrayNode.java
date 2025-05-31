package com.drakou111.neohouse.compilers.ast.expr.constants;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.lexer.Token;

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