package com.drakou111.neohouse.compilers.ast.types;

import com.drakou111.neohouse.compilers.ast.enums.BaseType;

public class BaseTypeNode extends TypeNode {
    BaseType type;

    public BaseTypeNode(BaseType type) {
        this.type = type;
    }
}
