package com.drakou111.neohouse.compiler.ast.expr;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.enums.VariableType;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;

public abstract class ExpressionNode extends ASTNode {
    public VariableType type;
    /**
     * If non-null, this expression is an enum value of the named enum. Enums are represented as
     * {@code long}s ({@link VariableType#LONG}) but carry their enum name for nominal type checks
     * (same-enum comparison only, no use in arithmetic). Only meaningful during parsing.
     */
    public String enumName = null;

    public ExpressionNode(VariableType type) {
        this.type = type;
    }

    @Override
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
