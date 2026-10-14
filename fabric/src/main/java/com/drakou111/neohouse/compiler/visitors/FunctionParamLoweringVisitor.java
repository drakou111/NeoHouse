package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.ParameterListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TupleFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.TypedFunctionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.helpers.HolderAndName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns every function into a void function without parameters: each parameter becomes a
 * compiler slot ({@link CompilerNames#parameterSlot}) that callers fill before the call.
 */
public class FunctionParamLoweringVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitTypedFunction(TypedFunctionNode function) {
        return lowered(function, function.parameters);
    }

    @Override
    public ASTNode visitTupleFunction(TupleFunctionNode function) {
        return lowered(function, function.parameters);
    }

    @Override
    public ASTNode visitVoidFunction(VoidFunctionNode function) {
        return lowered(function, function.parameters);
    }

    /** {@code function} as a void function without parameters, reading them from its parameter slots instead. */
    private static VoidFunctionNode lowered(FunctionNode function, ParameterListNode parameters) {
        Map<HolderAndName, String> renameMap = new HashMap<>();
        List<VariableNode> params = parameters.parameters;
        for (int i = 0; i < params.size(); i++) {
            VariableNode param = params.get(i);
            renameMap.put(new HolderAndName(param.holder, param.name), CompilerNames.parameterSlot(function.name, i));
        }
        StatementListNode body = (StatementListNode) new FunctionBodyRewriter(renameMap).visitStatements(function.statements);
        return new VoidFunctionNode(function.name, function.description, function.icon, new ParameterListNode(List.of()), body, function.token);
    }
}
