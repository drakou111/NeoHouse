package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.helpers.HolderAndName;

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
            return new VariableNode(var.type, newRef.holder, newRef.name, var.token);
        }
        return var;
    }
}