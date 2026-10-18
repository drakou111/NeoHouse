package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.TupleReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.TypedReturnNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.Holder;
import com.drakou111.neohouse.compiler.helpers.HolderAndName;

import java.util.ArrayList;
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
            VariableNode renamed = new VariableNode(var.type, var.holder, newName, var.teamName, var.token);
            renamed.fallback = var.fallback;
            return renamed;
        }
        return var;
    }

    @Override
    public ASTNode visitConstantPrimitive(ConstantPrimitiveNode constant) {
        return PlaceholderText.renamed(constant, (holder, name) -> renameMap.get(new HolderAndName(holder, name)));
    }

    @Override
    public ASTNode visitTypedReturn(TypedReturnNode ret) {
        NormalExpressionNode rewrittenExpr = (NormalExpressionNode) ret.expression.accept(this);

        VariableNode outVar = new VariableNode(ret.expression.type, Holder.PLAYER, "o$", null, ret.token);
        AssignationNode assignToO = new AssignationNode(outVar, AssignmentOperator.ASSIGN, rewrittenExpr, ret.token);

        VoidReturnNode voidReturn = new VoidReturnNode(ret.token);
        return new StatementListNode(List.of(assignToO, voidReturn));
    }

    @Override
    public ASTNode visitTupleReturn(TupleReturnNode ret) {
        List<AssignationNode> assigns = new ArrayList<>();
        for (int i = 0; i < ret.expressions.size(); i++) {
            NormalExpressionNode rewritten = (NormalExpressionNode) ret.expressions.get(i).accept(this);
            VariableNode outVar = new VariableNode(rewritten.type, Holder.PLAYER, "o$" + i, null, ret.token);
            assigns.add(new AssignationNode(outVar, AssignmentOperator.ASSIGN, rewritten, ret.token));
        }
        List<com.drakou111.neohouse.compiler.ast.main.StatementNode> stmts = new ArrayList<>(assigns);
        stmts.add(new VoidReturnNode(ret.token));
        return new StatementListNode(stmts);
    }
}
