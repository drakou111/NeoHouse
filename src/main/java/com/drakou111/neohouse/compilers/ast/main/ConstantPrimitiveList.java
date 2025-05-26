package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.constants.ConstantPrimitive;
import com.drakou111.neohouse.compilers.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class ConstantPrimitiveList extends ASTNode {
    public final List<ConstantPrimitive> constants;

    public ConstantPrimitiveList(final List<ConstantPrimitive> constants) {
        this.constants = new ArrayList<>(constants);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPrimitiveConstants(this);
    }

    @Override
    public String toString() {
        return "ConstantPrimitiveList [constants=" + constants + "]";
    }

    @Override
    public String toShortString() {
        return "ConstList[consts=" + StringHelper.constListToString(constants) + "]";
    }
}
