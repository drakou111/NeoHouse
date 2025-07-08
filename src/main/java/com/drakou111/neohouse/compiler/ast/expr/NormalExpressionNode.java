package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.enums.VariableType;

public abstract class NormalExpressionNode extends ExpressionNode {

    public NormalExpressionNode(VariableType type) {
        super(type);
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
