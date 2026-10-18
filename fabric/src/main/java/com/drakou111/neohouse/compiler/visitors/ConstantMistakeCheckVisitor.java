package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BinaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.RepeatNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.compiler.enums.BinaryOperator;
import com.drakou111.neohouse.compiler.enums.UnaryOperator;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.List;

/**
 * Catches mistakes that are plain from constants alone: dividing by a constant zero (an error) and a
 * repeat that runs zero times (a warning). Runs on the tree as written, where constants are already
 * replaced by their values. Read-only.
 */
public class ConstantMistakeCheckVisitor extends DefaultVisitor {
    private final List<CompileWarning> warnings;

    public ConstantMistakeCheckVisitor(List<CompileWarning> warnings) {
        this.warnings = warnings;
    }

    @Override
    public MainNode analyze(MainNode main) {
        main.accept(this);
        return main;
    }

    @Override
    public ASTNode visitBinaryExpr(BinaryExpressionNode binary) {
        if ((binary.operator == BinaryOperator.DIVIDE || binary.operator == BinaryOperator.MODULO) && isZero(binary.right))
            throw divisionByZero(binary.token, binary.operator == BinaryOperator.MODULO);
        return super.visitBinaryExpr(binary);
    }

    @Override
    public ASTNode visitAssignation(AssignationNode assignment) {
        boolean divides = assignment.operator == AssignmentOperator.DIV_ASSIGN || assignment.operator == AssignmentOperator.MOD_ASSIGN;
        if (divides && isZero(assignment.value))
            throw divisionByZero(assignment.token, assignment.operator == AssignmentOperator.MOD_ASSIGN);
        return super.visitAssignation(assignment);
    }

    @Override
    public ASTNode visitRepeat(RepeatNode repeat) {
        if (repeat.count == 0) this.warnings.add(new CompileWarning(repeat.token, "repeat(0) never runs its body."));
        return super.visitRepeat(repeat);
    }

    private static SemanticException divisionByZero(Token token, boolean remainder) {
        return new SemanticException(token, (remainder ? "The remainder of a division by zero" : "Dividing by zero")
                + " has no value. Check the number after the " + (remainder ? "%" : "/") + ".");
    }

    private static boolean isZero(NormalExpressionNode expression) {
        if (expression instanceof UnaryExpressionNode unary && unary.operator == UnaryOperator.NEGATE)
            return isZero(unary.operand);
        if (!(expression instanceof ConstantPrimitiveNode constant)) return false;
        return switch (constant.value) {
            case LongValue number -> number.value == 0;
            case DoubleValue number -> number.value == 0.0;
            default -> false;
        };
    }
}
