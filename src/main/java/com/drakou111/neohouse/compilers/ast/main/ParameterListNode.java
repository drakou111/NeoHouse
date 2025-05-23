package com.drakou111.neohouse.compilers.ast.main;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.ASTVisitor;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;

import java.util.ArrayList;
import java.util.List;

public class ParameterListNode extends ASTNode {
    public final List<VariableNode> parameters;

    public ParameterListNode(final List<VariableNode> parameters) {
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitParameters(this);
    }
}
