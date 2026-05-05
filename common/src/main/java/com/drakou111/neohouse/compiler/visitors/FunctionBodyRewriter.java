package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.helpers.HolderAndName;

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
            return new VariableNode(var.type, var.holder, newName, var.teamName, var.token);
        }
        return var;
    }

    @Override
    public ASTNode visitTypedReturn(TypedReturnNode ret) {
        NormalExpressionNode rewrittenExpr = (NormalExpressionNode) ret.expression.accept(this);

        VariableNode outVar = new VariableNode(ret.expression.type, Holder.PLAYER, "o$", null, ret.token);
        AssignationNode assignToO = new AssignationNode(outVar, AssignmentOperator.ASSIGN, rewrittenExpr, ret.token);

        VoidReturnNode voidReturn = new VoidReturnNode(ret.token);
        return new StatementListNode(List.of(assignToO, voidReturn));
    }
}
