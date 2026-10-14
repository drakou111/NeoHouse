package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Removes no-op assignments that have no runtime effect:
 *   x = x         (assign variable to itself)
 *   x += 0 / x -= 0
 *   x *= 1 / x /= 1
 */
public class NeutralStatementRemovalVisitor extends DefaultVisitor {

    @Override
    public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode stmt : statements.statements) {
            ASTNode visited = stmt.accept(this);
            if (visited instanceof StatementListNode slab) {
                result.addAll(slab.statements);
            } else if (visited != null) {
                result.add((StatementNode) visited);
            }
        }
        return new StatementListNode(result);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode node) {
        if (isNeutral(node)) return new StatementListNode(List.of());
        return node;
    }

    private boolean isNeutral(AssignationNode node) {
        VariableNode lhs = node.variable;

        // x = x
        if (node.operator == AssignmentOperator.ASSIGN
                && node.value instanceof VariableNode rhs
                && lhs.holder == rhs.holder
                && lhs.name.equals(rhs.name)
                && Objects.equals(lhs.teamName, rhs.teamName)
                && lhs.castSuffix == null && rhs.castSuffix == null) {
            return true;
        }

        // x += 0 / x -= 0
        if ((node.operator == AssignmentOperator.ADD_ASSIGN || node.operator == AssignmentOperator.SUB_ASSIGN)
                && node.value instanceof ConstantPrimitiveNode c) {
            return isZero(c);
        }

        // x *= 1 / x /= 1
        if ((node.operator == AssignmentOperator.MUL_ASSIGN || node.operator == AssignmentOperator.DIV_ASSIGN)
                && node.value instanceof ConstantPrimitiveNode c) {
            return isOne(c);
        }

        return false;
    }

    private boolean isZero(ConstantPrimitiveNode c) {
        return switch (c.value) {
            case LongValue l   -> l.value == 0L;
            case DoubleValue d -> d.value == 0.0;
            default -> false;
        };
    }

    private boolean isOne(ConstantPrimitiveNode c) {
        return switch (c.value) {
            case LongValue l   -> l.value == 1L;
            case DoubleValue d -> d.value == 1.0;
            default -> false;
        };
    }
}
