package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.ast.stmt.StmtNode;

import java.util.List;

public class BlockNode extends ASTNode {
    public final List<StmtNode> statements;

    public BlockNode(List<StmtNode> statements) {
        this.statements = statements;
    }

    @Override
    public String toString() {
        return "BlockNode{" + statements + '}';
    }
}
