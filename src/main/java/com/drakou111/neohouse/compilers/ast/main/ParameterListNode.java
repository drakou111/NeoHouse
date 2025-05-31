package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.helpers.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class ParameterListNode extends ASTNode {
    public final List<VariableNode> parameters;

    public ParameterListNode(final List<VariableNode> parameters) {
        if (parameters == null || parameters.isEmpty())
            this.parameters = new ArrayList<>();
        else
            this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitParameters(this);
    }

    @Override
    public String toString() {
        return "ParameterListNode[parameters=" + parameters + "]";
    }

    @Override
    public String toShortString() {
        return "ParamList[params=" + StringHelper.nodeListToString(parameters) + "]";
    }
}
