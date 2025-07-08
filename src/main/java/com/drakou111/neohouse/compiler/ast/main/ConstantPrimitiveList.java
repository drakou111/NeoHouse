package com.drakou111.neohouse.compiler.ast.main;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.visitors.ASTVisitor;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class ConstantPrimitiveList extends ASTNode {
    public final List<ConstantPrimitiveNode> constants;

    public ConstantPrimitiveList(final List<ConstantPrimitiveNode> constants) {
        if (constants == null || constants.isEmpty())
            this.constants = new ArrayList<>();
        else
            this.constants = new ArrayList<>(constants);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPrimitiveConstants(this);
    }

    @Override
    public String toString() {
        return "ConstantPrimitiveList[constants=" + constants + "]";
    }

    @Override
    public String toShortString() {
        return "ConstList[consts=" + StringHelper.nodeListToString(constants) + "]";
    }
}
