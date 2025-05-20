package com.drakou111.neohouse.compilers.ast.stmt;

import com.drakou111.neohouse.compilers.ast.expr.VarNode;
import com.drakou111.neohouse.compilers.ast.types.TypeNode;

class DefinitionNode extends StmtNode {
    TypeNode type;
    VarNode variable;
}
