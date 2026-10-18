package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.enums.AssignmentOperator;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ++ and -- work on doubles, stepping by 1.0. */
class DoubleIncrementTest {

    private static AssignationNode lastAssignment(String body) {
        MainNode main = TestCompiler.analyze("double p.d;\nevent onJoin { p.d = 0.5; " + body + " }");
        for (MainStatementNode statement : main.statements) {
            if (!(statement instanceof EventNode event)) continue;
            StatementNode last = event.statements.statements.getLast();
            return (AssignationNode) last;
        }
        throw new AssertionError("no event");
    }

    @Test
    void incrementAddsOnePointZero() {
        AssignationNode step = lastAssignment("p.d++;");

        assertThat(step.operator).isEqualTo(AssignmentOperator.ADD_ASSIGN);
        assertThat(step.value).isInstanceOfSatisfying(ConstantPrimitiveNode.class,
                one -> assertThat(one.value).isInstanceOfSatisfying(DoubleValue.class, d -> assertThat(d.value).isEqualTo(1.0)));
    }

    @Test
    void decrementSubtractsOnePointZero() {
        assertThat(lastAssignment("p.d--;").operator).isEqualTo(AssignmentOperator.SUB_ASSIGN);
    }

    @Test
    void plusEqualsAWholeNumberIsStillAnError() {
        assertThatThrownBy(() -> lastAssignment("p.d += 1;"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Cannot assign a LONG into a DOUBLE");
    }
}
