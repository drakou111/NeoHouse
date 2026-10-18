package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.UnaryExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.BoolValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.LongValue;
import com.drakou111.neohouse.compiler.ast.expr.constants.StringValue;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.enums.BooleanOperator;
import com.drakou111.neohouse.compiler.enums.LogicalOperator;
import com.drakou111.neohouse.compiler.enums.UnaryOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An if whose condition only involves constants (like {@code if (1 == 1)} or {@code if (DEBUG)}) is
 * decided here: the branch that would run takes the if's place and the other is dropped, saving
 * Housing a conditional and the temporary variables. Each one is reported, since it is usually a
 * mistake or leftover debug code. Runs after type inference, so both sides have matching types.
 */
public class ConstantConditionFoldingVisitor extends DefaultVisitor {
    private final List<CompileWarning> warnings;

    public ConstantConditionFoldingVisitor(List<CompileWarning> warnings) {
        this.warnings = warnings;
    }

    @Override
    public MainNode analyze(MainNode main) {
        return (MainNode) main.accept(this);
    }

    @Override
    public ASTNode visitStatements(StatementListNode statements) {
        List<StatementNode> result = new ArrayList<>();
        for (StatementNode statement : statements.statements) {
            Optional<Boolean> decided = statement instanceof ConditionalNode conditional && conditional.condition != null
                    ? valueOf(conditional.condition)
                    : Optional.empty();
            if (decided.isEmpty()) {
                result.add((StatementNode) statement.accept(this));
                continue;
            }

            ConditionalNode conditional = (ConditionalNode) statement;
            boolean always = decided.get();
            // An if without an else has an empty else block.
            boolean hasElse = conditional.elseBlock != null && !conditional.elseBlock.statements.isEmpty();
            this.warnings.add(new CompileWarning(conditional.token, always
                    ? "This condition is always true, so its actions always run" + (hasElse ? " and the else never does." : ".")
                    : "This condition is always false, so its actions never run" + (hasElse ? " and the else always does." : ".")));
            StatementListNode kept = always ? conditional.thenBlock : conditional.elseBlock;
            if (kept != null) result.addAll(((StatementListNode) kept.accept(this)).statements);
        }
        return new StatementListNode(result);
    }

    /** The condition's value when it only involves constants, else empty. */
    private static Optional<Boolean> valueOf(ExpressionNode condition) {
        return switch (condition) {
            case ConstantPrimitiveNode constant when constant.value instanceof BoolValue bool -> Optional.of(bool.value);
            case UnaryExpressionNode unary when unary.operator == UnaryOperator.NOT -> valueOf(unary.operand).map(value -> !value);
            case BooleanExpressionNode comparison
                    when comparison.left instanceof ConstantPrimitiveNode left && comparison.right instanceof ConstantPrimitiveNode right ->
                    compare(left, right, comparison.operator);
            case LogicalExpressionNode logical -> combine(logical);
            default -> Optional.empty();
        };
    }

    private static Optional<Boolean> combine(LogicalExpressionNode logical) {
        boolean and = logical.operator == LogicalOperator.AND;
        boolean result = and;
        for (ExpressionNode part : logical.conditions) {
            Optional<Boolean> value = valueOf(part);
            if (value.isEmpty()) return Optional.empty();
            result = and ? result && value.get() : result || value.get();
        }
        return Optional.of(result);
    }

    private static Optional<Boolean> compare(ConstantPrimitiveNode left, ConstantPrimitiveNode right, BooleanOperator operator) {
        if (left.value instanceof LongValue a && right.value instanceof LongValue b) return Optional.of(order(Long.compare(a.value, b.value), operator));
        if (left.value instanceof DoubleValue a && right.value instanceof DoubleValue b) return Optional.of(order(Double.compare(a.value, b.value), operator));
        Object a = valueObject(left);
        Object b = valueObject(right);
        if (a == null || b == null) return Optional.empty();
        return switch (operator) {
            case EQUALS -> Optional.of(a.equals(b));
            case NOT_EQUALS -> Optional.of(!a.equals(b));
            default -> Optional.empty();
        };
    }

    private static Object valueObject(ConstantPrimitiveNode constant) {
        return switch (constant.value) {
            case StringValue text -> text.value;
            case BoolValue bool -> bool.value;
            default -> null;
        };
    }

    private static boolean order(int comparison, BooleanOperator operator) {
        return switch (operator) {
            case EQUALS -> comparison == 0;
            case NOT_EQUALS -> comparison != 0;
            case LESS_THAN -> comparison < 0;
            case GREATER_THAN -> comparison > 0;
            case LESS_THAN_EQUALS -> comparison <= 0;
            case GREATER_THAN_EQUALS -> comparison >= 0;
        };
    }
}
