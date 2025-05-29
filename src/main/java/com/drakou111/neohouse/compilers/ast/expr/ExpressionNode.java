package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;

public abstract class ExpressionNode extends ASTNode {
    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
