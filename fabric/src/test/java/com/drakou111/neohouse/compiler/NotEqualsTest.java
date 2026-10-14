package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.enums.BooleanOperator;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.constants.conditions.VariableInputs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotEqualsTest {
    private static final String SOURCE = "function f() { if (p.x != 3) { p.y = 1; } }";

    private static void names(Action action, List<String> into) {
        into.add(action.getDebugName());
        for (Action child : action.getChildren()) names(child, into);
    }

    @Test
    void parsesAsNotEquals() {
        VoidFunctionNode function = (VoidFunctionNode) TestCompiler.analyze(SOURCE).statements.getFirst();
        ConditionalNode conditional = (ConditionalNode) function.statements.statements.getFirst();
        BooleanExpressionNode comparison = (BooleanExpressionNode) ((LogicalExpressionNode) conditional.condition).conditions.getFirst();

        assertThat(comparison.operator).isEqualTo(BooleanOperator.NOT_EQUALS);
    }

    @Test
    void generatesAnInvertedEqualityCondition() {
        List<String> names = new ArrayList<>();
        names(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(SOURCE)), names);

        assertThat(names).contains("Search: " + VariableInputs.INVERT);
    }

    @Test
    void equalsIsNotInverted() {
        List<String> names = new ArrayList<>();
        names(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(SOURCE.replace("!=", "=="))), names);

        assertThat(names).doesNotContain("Search: " + VariableInputs.INVERT);
    }
}
