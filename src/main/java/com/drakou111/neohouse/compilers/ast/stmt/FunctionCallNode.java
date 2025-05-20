package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.expr.ExprNode;

import java.util.List;

class FunctionCallNode extends StmtNode {
    String functionName;
    List<ExprNode> arguments;
    boolean isBroadcast;
}
