package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.AssignmentOperator;
import com.drakou111.neohouse.compilers.ast.enums.Holder;
import com.drakou111.neohouse.compilers.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.StatementListNode;
import com.drakou111.neohouse.compilers.ast.statement.AssignationNode;
import com.drakou111.neohouse.compilers.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compilers.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compilers.helpers.HolderAndName;

import java.util.List;
import java.util.Map;

public class FunctionBodyRewriter extends DefaultVisitor {
    private final Map<HolderAndName, String> renameMap;

    FunctionBodyRewriter(Map<HolderAndName, String> renameMap) {
        this.renameMap = renameMap;
    }

    @Override
    public ASTNode visitVariable(VariableNode var) {
        HolderAndName key = new HolderAndName(var.holder, var.name);
        if (renameMap.containsKey(key)) {
            String newName = renameMap.get(key);
            return new VariableNode(var.type, var.holder, newName, var.token);
        }
        return var;
    }

    @Override
    public ASTNode visitTypedReturn(TypedReturnNode ret) {
        NormalExpressionNode rewrittenExpr = (NormalExpressionNode) ret.expression.accept(this);

        VariableNode outVar = new VariableNode(ret.expression.type, Holder.PLAYER, "o$", ret.token);
        AssignationNode assignToO = new AssignationNode(outVar, AssignmentOperator.ASSIGN, rewrittenExpr, ret.token);

        VoidReturnNode voidReturn = new VoidReturnNode(ret.token);
        return new StatementListNode(List.of(assignToO, voidReturn));
    }
}
