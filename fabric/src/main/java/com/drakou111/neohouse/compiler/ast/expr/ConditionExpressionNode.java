package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public abstract class ConditionExpressionNode extends ExpressionNode {

    public ConditionExpressionNode(VariableType type) {
        super(type);
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
