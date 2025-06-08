package com.drakou111.neohouse.compiler.ast.main;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.ASTVisitor;

public abstract class StatementNode extends ASTNode {
    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
