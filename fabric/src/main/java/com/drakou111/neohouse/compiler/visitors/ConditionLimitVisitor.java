package com.drakou111.neohouse.compiler.visitors;

import com.drakou111.neohouse.compiler.ast.ASTNode;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ConditionCallNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.exceptions.SemanticException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Rejects a conditional holding more conditions of one type than Housing allows
 * ({@link AddCondition#maxCount}). Runs on the lowered tree, where every condition is final.
 */
public class ConditionLimitVisitor extends DefaultVisitor {

    @Override
    public MainNode analyze(MainNode main) {
        main.accept(this);
        return main;
    }

    @Override
    public ASTNode visitConditional(ConditionalNode conditional) {
        if (conditional.condition != null) checkCounts(conditional.condition);
        return super.visitConditional(conditional);
    }

    private static void checkCounts(ExpressionNode condition) {
        List<ExpressionNode> conditions = condition instanceof LogicalExpressionNode logical ? logical.conditions : List.of(condition);
        Map<AddCondition, Integer> counts = new EnumMap<>(AddCondition.class);
        for (ExpressionNode single : conditions) {
            AddCondition kind = kindOf(single);
            if (kind == null) continue;
            int count = counts.merge(kind, 1, Integer::sum);
            if (count > kind.maxCount)
                throw new SemanticException(single.token, "A conditional can hold at most %d '%s' conditions; this one has more."
                        .formatted(kind.maxCount, kind.displayName));
        }
    }

    private static AddCondition kindOf(ExpressionNode condition) {
        return switch (condition) {
            case ConditionCallNode call -> AddCondition.requireByName(call.name);
            case BooleanExpressionNode comparison when comparison.left instanceof VariableNode -> AddCondition.VARIABLE;
            case BooleanExpressionNode comparison when comparison.left instanceof PlaceholderNode -> AddCondition.PLACEHOLDER;
            default -> null;
        };
    }
}
