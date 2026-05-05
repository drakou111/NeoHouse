package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public abstract class ExpressionNode extends ASTNode {
    public VariableType type;

    public ExpressionNode(VariableType type) {
        this.type = type;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
