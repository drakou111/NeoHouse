package com.drakou111.neohouse.compilers.ast;

import com.drakou111.neohouse.compilers.ast.expr.VarNode;
import com.drakou111.neohouse.compilers.ast.types.TypeNode;

public class ParamNode extends ASTNode {
    public TypeNode type;
    public VarNode variable;
}
