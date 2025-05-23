package com.drakou111.neohouse.compilers.ast.statement;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class MainNode extends ASTNode {
    public final List<MainStatementNode> statements;

    public MainNode(List<MainStatementNode> statements) {
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitMain(this);
    }
}
