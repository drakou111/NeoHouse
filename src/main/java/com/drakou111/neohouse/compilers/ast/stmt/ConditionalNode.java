package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.BlockNode;
import com.drakou111.neohouse.compilers.ast.expr.ExprNode;

class ConditionalNode extends StmtNode {
    ExprNode condition;
    BlockNode thenBranch;
    BlockNode elseBranch;  // nullable
}
