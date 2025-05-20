package com.drakou111.neohouse.compilers.ast.expr;

import com.drakou111.neohouse.compilers.ast.enums.HolderType;

public class VarNode extends ExprNode {
    HolderType holder; // enum {P, G, T}
    String name;
}
