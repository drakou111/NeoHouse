package com.drakou111.neohouse.compilers.ast.expr;

class BinaryOpNode extends ExprNode {
    ExprNode left;
    String operator;  // "+", "-", "*", "/", "==", etc.
    ExprNode right;
}
