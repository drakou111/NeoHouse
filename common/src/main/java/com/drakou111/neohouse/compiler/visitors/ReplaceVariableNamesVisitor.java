package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.helpers.HolderAndName;

import java.util.Map;

public class ReplaceVariableNamesVisitor extends DefaultVisitor {
    private final Map<HolderAndName, HolderAndName> nameMap;

    public ReplaceVariableNamesVisitor(Map<HolderAndName, HolderAndName> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public ASTNode visitVariable(VariableNode var) {
        HolderAndName key = new HolderAndName(var.holder, var.name);
        if (nameMap.containsKey(key)) {
            HolderAndName newRef = nameMap.get(key);
            return new VariableNode(var.type, newRef.holder, newRef.name, var.teamName, var.token);
        }
        return var;
    }
}