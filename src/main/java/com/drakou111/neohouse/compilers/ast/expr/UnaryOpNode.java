package com.drakou111.neohouse.compilers.ast.expr;

public class UnaryOpNode extends ExprNode {
    String operator;  // "-", "!", "++", "--", etc.
    ExprNode operand;
}
