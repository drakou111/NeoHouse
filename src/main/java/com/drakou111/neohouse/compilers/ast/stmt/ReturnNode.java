package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.expr.ExprNode;

class ReturnNode extends StmtNode {
    ExprNode returnValue;  // nullable (for bare 'return')
}
