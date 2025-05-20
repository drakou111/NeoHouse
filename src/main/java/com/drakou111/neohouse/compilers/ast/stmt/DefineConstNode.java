package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.expr.ConstExprNode;

class DefineConstNode extends StmtNode {
    String name;
    ConstExprNode value;
}