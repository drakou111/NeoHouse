package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;

public abstract class StatementNode extends ASTNode {
    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
