package com.drakou111.neohouse.compilers.ast.expr.var;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.main.ConstantPrimitiveList;

public class PlaceholderNode extends ExpressionNode {

    public final String placeholder;
    public final ConstantPrimitiveList constants;

    public PlaceholderNode(final String placeholder, final ConstantPrimitiveList constants) {
        this.placeholder = placeholder;
        this.constants = constants;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPlaceholder(this);
    }
}
