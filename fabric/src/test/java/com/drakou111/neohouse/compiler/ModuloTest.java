package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuloTest {

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
            case VariableNode variable -> variable.name + (variable.castSuffix == null ? "" : "(" + variable.castSuffix + ")");
            case ConstantPrimitiveNode constant -> constant.value.toString();
            default -> value.getClass().getSimpleName();
        };
    }

    @Test
    void longModuloAssignment() {
        assertThat(loweredBody("long p.a; long p.b;", "p.a %= p.b;")).containsExactly(
                "f$t0 ASSIGN a",
                "f$t0 DIV_ASSIGN b",
                "f$t0 MUL_ASSIGN b",
                "a SUB_ASSIGN f$t0");
    }

    @Test
    void doubleModuloTruncatesOnlyTheQuotient() {
        assertThat(loweredBody("double p.a; double p.b;", "p.a %= p.b;")).containsExactly(
                "f$t0 ASSIGN a",
                "f$t0 DIV_ASSIGN b",
                "f$t0 ASSIGN f$t0(L)",
                "f$t0 ASSIGN f$t0(D)",
                "f$t0 MUL_ASSIGN b",
                "a SUB_ASSIGN f$t0");
    }

    @Test
    void moduloInsideAnExpression() {
        assertThat(loweredBody("long p.a; long p.r;", "p.r = p.a % 4 + 1;")).containsExactly(
                "f$t0 ASSIGN a",
                "f$t1 ASSIGN f$t0",
                "f$t1 DIV_ASSIGN 4",
                "f$t1 MUL_ASSIGN 4",
                "f$t0 SUB_ASSIGN f$t1",
                "f$t0 ADD_ASSIGN 1",
                "r ASSIGN f$t0");
    }

    @Test
    void moduloOfStringsIsRejected() {
        assertThatThrownBy(() -> loweredBody("string p.s;", "p.s %= \"x\";")).isInstanceOf(CompilingException.class);
    }

    @Test
    void moduloAssignmentOnVarIsRejected() {
        assertThatThrownBy(() -> loweredBody("var p.v;", "p.v %= 3;"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("not var");
    }

    @Test
    void moduloWithVarOperandIsRejected() {
        assertThatThrownBy(() -> loweredBody("long p.a; var p.v;", "p.a = p.a % p.v;"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("not var");
    }

    @Test
    void castVarCanBeUsedWithModulo() {
        assertThat(loweredBody("long p.r; var p.v;", "p.r = (long) p.v % 3;")).endsWith("r ASSIGN f$t0");
    }
}
