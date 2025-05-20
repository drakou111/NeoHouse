package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.ast.expr.ExprNode;
import com.drakou111.neohouse.compilers.ast.expr.VarNode;

public class AssignmentNode extends ASTNode {
    VarNode target;
    ExprNode value;
}
