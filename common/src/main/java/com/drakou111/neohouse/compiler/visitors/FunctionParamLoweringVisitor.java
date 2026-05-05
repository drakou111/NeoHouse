package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.helpers.HolderAndName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionParamLoweringVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitTypedFunction(TypedFunctionNode function) {
        Map<HolderAndName, String> renameMap = new HashMap<>();
        List<VariableNode> params = function.parameters.parameters;
        for (int i = 0; i < params.size(); i++) {
            VariableNode param = params.get(i);
            renameMap.put(new HolderAndName(param.holder, param.name), function.name + "_p$" + i);
        }

        StatementListNode rewrittenBody = (StatementListNode) new FunctionBodyRewriter(renameMap).visitStatements(function.statements);

        return new VoidFunctionNode(function.name, function.description, new ParameterListNode(List.of()), rewrittenBody, function.token);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode function) {
        Map<HolderAndName, String> renameMap = new HashMap<>();
        List<VariableNode> params = function.parameters.parameters;
        for (int i = 0; i < params.size(); i++) {
            VariableNode param = params.get(i);
            renameMap.put(new HolderAndName(param.holder, param.name), function.name + "_p$" + i);
        }

        StatementListNode rewrittenBody = (StatementListNode) new FunctionBodyRewriter(renameMap).visitStatements(function.statements);

        return new VoidFunctionNode(function.name, function.description, new ParameterListNode(List.of()), rewrittenBody, function.token);
    }

}
