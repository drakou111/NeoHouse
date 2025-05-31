package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;

public abstract class ExpressionNode extends ASTNode {
    public VariableType type;

    public ExpressionNode(VariableType type) {
        this.type = type;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
