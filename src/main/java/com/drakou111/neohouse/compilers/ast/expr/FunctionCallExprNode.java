package com.drakou111.neohouse.compilers.ast.expr;

import java.util.List;

public class FunctionCallExprNode extends ExprNode {
    String functionName;
    List<ExprNode> arguments;
}
