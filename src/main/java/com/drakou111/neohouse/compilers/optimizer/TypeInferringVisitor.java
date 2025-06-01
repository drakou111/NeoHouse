package com.drakou111.neohouse.compilers.optimizer;

import com.drakou111.neohouse.compilers.ast.ASTNode;
import com.drakou111.neohouse.compilers.ast.enums.VariableType;
import com.drakou111.neohouse.compilers.ast.expr.*;
import com.drakou111.neohouse.compilers.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compilers.ast.main.MainNode;
import com.drakou111.neohouse.compilers.ast.statement.AssignationNode;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;

public class TypeInferringVisitor extends DefaultVisitor {
    public MainNode analyze(MainNode main) {
        return (MainNode)main.accept(this);
    }

    @Override
    public ASTNode visitBooleanExpr(BooleanExpressionNode node) {
        return new BooleanExpressionNode(
                (NormalExpressionNode) node.left.accept(this),
                (NormalExpressionNode) node.right.accept(this),
                node.operator,
                node.token
        );
    }

    @Override
    public ASTNode visitLogicalExpr(LogicalExpressionNode node) {
        List<BooleanExpressionNode> resolved = new ArrayList<>();
        for (BooleanExpressionNode cond : node.conditions) {
            resolved.add((BooleanExpressionNode) cond.accept(this));
        }
        return new LogicalExpressionNode(resolved, node.operator, node.token);
    }

    @Override
    public ASTNode visitUnaryExpr(UnaryExpressionNode node) {
        NormalExpressionNode expr = (NormalExpressionNode) node.operand.accept(this);
        return new UnaryExpressionNode(node.operator, expr, expr.type, node.token);
    }

    @Override
    public ASTNode visitBinaryExpr(BinaryExpressionNode node) {
        NormalExpressionNode left = (NormalExpressionNode) node.left.accept(this);
        NormalExpressionNode right = (NormalExpressionNode) node.right.accept(this);

        VariableType resultType;
        if (left.type == VariableType.VAR || right.type == VariableType.VAR) {
            resultType = VariableType.VAR;
        } else if (left.type == right.type) {
            resultType = left.type;
        } else {
            throw new SemanticException(node.token, "Type mismatch in binary expression: left is " + left.type + ", right is " + right.type);
        }

        return new BinaryExpressionNode(left, right, node.operator, resultType, node.token);
    }


}
