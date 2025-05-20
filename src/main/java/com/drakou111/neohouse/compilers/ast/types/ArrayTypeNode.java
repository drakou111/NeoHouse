package com.drakou111.neohouse.compilers.ast.types;

import com.drakou111.neohouse.compilers.ast.expr.ConstExprNode;

public class ArrayTypeNode extends TypeNode {
    BaseTypeNode baseType;
    ConstExprNode size;

    public ArrayTypeNode(BaseTypeNode baseType, ConstExprNode size) {
        this.baseType = baseType;
        this.size = size;
    }
}
