package com.drakou111.neohouse.compilers.ast.expr.var;

import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitive;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderNode extends ExpressionNode {

    public final String placeholder;
    public final List<ConstantPrimitive> constants;

    public PlaceholderNode(String placeholder, List<ConstantPrimitive> constants) {
        this.placeholder = placeholder;
        this.constants = new ArrayList<>(constants);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPlaceholder(this);
    }
}
