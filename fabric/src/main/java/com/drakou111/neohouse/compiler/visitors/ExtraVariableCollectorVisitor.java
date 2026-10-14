package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtraVariableCollectorVisitor extends EmptyVisitor<Void> {
    private final Set<String> names = new LinkedHashSet<>();

    public List<String> collect(MainNode main) {
        main.accept(this);
        return new ArrayList<>(names);
    }

    @Override
    public Void visitVariable(VariableNode variable) {
        if (variable.name.contains("$")) {
            names.add(variable.name);
        }
        return null;
    }
}
