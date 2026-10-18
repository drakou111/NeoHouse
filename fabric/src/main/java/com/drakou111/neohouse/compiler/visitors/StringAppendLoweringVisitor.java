package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.BinaryOperator;
import com.drakou111.neohouse.compiler.enums.VariableType;

/**
 * Turns {@code s += x} on a string variable into {@code s = s + x}. Housing's Change Variable action
 * only increments numbers, so appending text has to be a Set of the joined text. Runs once variable
 * types are known (after scope checking) and before type inference checks the result.
 */
public class StringAppendLoweringVisitor extends DefaultVisitor {

    @Override
    public MainNode analyze(MainNode main) {
        return (MainNode) main.accept(this);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignment) {
        NormalExpressionNode value = (NormalExpressionNode) assignment.value.accept(this);
        VariableNode target = assignment.variable;
        if (assignment.operator != AssignmentOperator.ADD_ASSIGN || target.type != VariableType.STRING)
            return new AssignationNode(target, assignment.operator, value, assignment.token);

        VariableNode read = new VariableNode(target.type, target.holder, target.name, target.teamName, target.token);
        read.wasBare = target.wasBare;
        BinaryExpressionNode joined = new BinaryExpressionNode(read, value, BinaryOperator.ADD, null, assignment.token);
        return new AssignationNode(target, AssignmentOperator.ASSIGN, joined, assignment.token);
    }
}
