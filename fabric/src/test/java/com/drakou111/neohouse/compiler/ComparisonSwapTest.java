package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.var.PlaceholderNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.enums.BooleanOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonSwapTest {

    private static VoidFunctionNode compile(String declarations, String condition) {
        return (VoidFunctionNode) TestCompiler.analyze(declarations + "\nfunction f() { if (" + condition + ") { p.y = 1; } }")
                .statements.getLast();
    }

    private static BooleanExpressionNode onlyComparison(VoidFunctionNode function) {
        ConditionalNode conditional = (ConditionalNode) function.statements.statements.getLast();
        ExpressionNode condition = conditional.condition;
        if (condition instanceof LogicalExpressionNode logical) condition = logical.conditions.getFirst();
        return (BooleanExpressionNode) condition;
    }

    @Test
    void placeholderOnTheLeftOfAVariableIsMirrored() {
        VoidFunctionNode function = compile("string p.name;", "ph.playerName() == p.name");

        BooleanExpressionNode comparison = onlyComparison(function);

        assertThat(function.statements.statements).hasSize(1);
        assertThat(comparison.left).isInstanceOf(VariableNode.class);
        assertThat(comparison.right).isInstanceOf(PlaceholderNode.class);
        assertThat(comparison.operator).isEqualTo(BooleanOperator.EQUALS);
    }

    @Test
    void mirroringFlipsOrderedComparisons() {
        BooleanExpressionNode comparison = onlyComparison(compile("double p.hp;", "ph.playerHealth() < p.hp"));

        assertThat(((VariableNode) comparison.left).name).isEqualTo("hp");
        assertThat(comparison.operator).isEqualTo(BooleanOperator.GREATER_THAN);
    }

    @Test
    void placeholderAgainstAConstantStillUsesATemporary() {
        VoidFunctionNode function = compile("", "ph.playerName() == \"TEST\"");

        assertThat(function.statements.statements).hasSize(2);
        assertThat(((VariableNode) onlyComparison(function).left).name).startsWith("f$t");
    }
}
