package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.BlockNode;
import com.drakou111.neohouse.compilers.ast.expr.ExprNode;

class RepeatNode extends StmtNode {
    ExprNode count;
    BlockNode body;
}