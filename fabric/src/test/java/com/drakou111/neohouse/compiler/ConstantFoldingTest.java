package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantFoldingTest {

    /** The lowered body of the script's only function, one "target op value" line per assignment. */
    private static List<String> loweredBody(String declarations, String body) {
        MainNode main = TestCompiler.analyze(declarations + "\nfunction f() {\n" + body + "\n}");
        VoidFunctionNode function = (VoidFunctionNode) main.statements.getLast();
        List<String> lines = new ArrayList<>();
        for (StatementNode statement : function.statements.statements) {
            AssignationNode assignment = (AssignationNode) statement;
            lines.add(assignment.variable.name + " " + assignment.operator + " " + render(assignment.value));
        }
        return lines;
    }

    private static String render(NormalExpressionNode value) {
        return switch (value) {
            case VariableNode variable -> variable.name;
            case ConstantPrimitiveNode constant -> constant.value.toString();
            default -> value.getClass().getSimpleName();
        };
    }

    @Test
    void doubleDivisionIsComputed() {
        assertThat(loweredBody("double p.x;", "p.x = 1.0 / 2.0;")).containsExactly("x ASSIGN 0.5");
    }

    @Test
    void constantPartsOfAnExpressionAreComputed() {
        assertThat(loweredBody("long p.a; long p.r;", "p.r = 2 + 3 * 4 + p.a;")).containsExactly(
                "f$t0 ASSIGN 14",
                "f$t0 ADD_ASSIGN a",
                "r ASSIGN f$t0");
    }

    @Test
    void longArithmeticTruncatesAndWraps() {
        assertThat(loweredBody("long p.x;", "p.x = 7 / 2;")).containsExactly("x ASSIGN 3");
        assertThat(loweredBody("long p.x;", "p.x = -7 % 3;")).containsExactly("x ASSIGN -1");
        assertThat(loweredBody("long p.x;", "p.x = 9223372036854775807 + 1;")).containsExactly("x ASSIGN -9223372036854775808");
    }

    @Test
    void doubleModulo() {
        assertThat(loweredBody("double p.x;", "p.x = 7.5 % 2.0;")).containsExactly("x ASSIGN 1.5");
    }

    @Test
    void constantCastsAreComputed() {
        assertThat(loweredBody("long p.x;", "p.x = (long) 2.7;")).containsExactly("x ASSIGN 2");
        assertThat(loweredBody("double p.x;", "p.x = (double) 5;")).containsExactly("x ASSIGN 5.0");
        assertThat(loweredBody("double p.x;", "p.x = (double)(long) 2.9 + 0.5;")).containsExactly("x ASSIGN 2.5");
    }

    @Test
    void divisionByAConstantZeroIsLeftToHousing() {
        assertThat(loweredBody("long p.x;", "p.x = 1 / 0;")).containsExactly(
                "f$t0 ASSIGN 1",
                "f$t0 DIV_ASSIGN 0",
                "x ASSIGN f$t0");
    }

    @Test
    void neutralStepsDisappearLeavingOneWrite() {
        assertThat(loweredBody("long p.a; long p.r;", "p.r = p.a * 1 + 0;")).containsExactly("r ASSIGN a");
    }
}
