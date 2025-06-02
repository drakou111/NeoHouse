package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class ArgumentListNode extends ASTNode {
    public final List<NormalExpressionNode> expressions;

    public ArgumentListNode(final List<NormalExpressionNode> expressions) {
        if (expressions == null || expressions.isEmpty())
            this.expressions = new ArrayList<>();
        else
            this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArguments(this);
    }

    @Override
    public String toString() {
        return "ArgumentListNode[expressions=" + expressions + "]";
    }

    @Override
    public String toShortString() {
        return "ArgList[exprs=" + StringHelper.nodeListToString(expressions) + "]";
    }
}
