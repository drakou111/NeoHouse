package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.BooleanExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.LogicalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidReturnNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Optimizations on compiler-made variables; the script's own variables keep every write. */
class TempOptimizationTest {

    private static final String SQUARE_PLUS_ONE = "function long sq(long x) { return x * x + 1; }\n";

    private static List<String> lowered(String source, String body) {
        MainNode main = TestCompiler.analyze(source);
        for (MainStatementNode statement : main.statements) {
            List<StatementNode> statements = switch (statement) {
                case EventNode e when e.event.name.equals(body) -> e.statements.statements;
                case FunctionNode f when f.name.equals(body) -> f.statements.statements;
                default -> null;
            };
            if (statements == null) continue;
            List<String> lines = new ArrayList<>();
            for (StatementNode s : statements) lines.add(render(s));
            return lines;
        }
        throw new AssertionError("no body named " + body);
    }

    private static List<String> onJoin(String declarations, String body) {
        return lowered(declarations + "\nevent onJoin {\n" + body + "\n}", "onJoin");
    }

    private static String render(StatementNode s) {
        return switch (s) {
            case AssignationNode a -> a.variable.name + " " + a.operator + " " + render(a.value);
            case VoidFunctionCallNode c -> "CALL " + c.functionName;
            case VoidReturnNode r -> "RETURN";
            case ConditionalNode c -> "IF " + render(c.condition) + " THEN " + c.thenBlock.statements.stream().map(TempOptimizationTest::render).toList();
            default -> s.getClass().getSimpleName();
        };
    }

    private static String render(ExpressionNode e) {
        return switch (e) {
            case LogicalExpressionNode l -> l.conditions.stream().map(TempOptimizationTest::render).toList().toString();
            case BooleanExpressionNode b -> render(b.left) + " " + b.operator + " " + render(b.right);
            case VariableNode v -> v.name + (v.castSuffix == null ? "" : "(" + v.castSuffix + ")");
            case ConstantPrimitiveNode c -> c.value.toString();
            case NormalExpressionNode n -> n.getClass().getSimpleName();
            default -> e.getClass().getSimpleName();
        };
    }

    @Test
    void scriptVariablesKeepEveryStep() {
        // p.r must not hold p.a in between: the sum is built in a temp.
        assertThat(onJoin("long p.a; long p.r;", "p.r = p.a + 1;")).containsExactly(
                "t$0 ASSIGN a",
                "t$0 ADD_ASSIGN 1",
                "r ASSIGN t$0");
    }

    @Test
    void stepsStartingFromTheVariableItselfAreDoneInPlace() {
        assertThat(onJoin("long p.a;", "p.a = p.a << 2;")).containsExactly("a LEFT_SHIFT_ASSIGN 2");
        assertThat(onJoin("long p.a; long p.b;", "p.a = (p.a + p.b) * 3;")).containsExactly(
                "a ADD_ASSIGN b",
                "a MUL_ASSIGN 3");
    }

    @Test
    void aStepThatReadsTheVariableKeepsTheTemp() {
        // Doing it in place would read the already-doubled p.a in the second step.
        assertThat(onJoin("long p.a;", "p.a = p.a * 2 + p.a;")).containsExactly(
                "t$0 ASSIGN a",
                "t$0 MUL_ASSIGN 2",
                "t$0 ADD_ASSIGN a",
                "a ASSIGN t$0");
    }

    @Test
    void aFallbackOnTheFirstReadKeepsTheTemp() {
        assertThat(onJoin("long p.a;", "p.a = p.a?5 + 1;")).first().asString().startsWith("t$0 ASSIGN");
    }

    @Test
    void aPlainCopyGoesStraightToTheVariable() {
        assertThat(onJoin("long p.a; long p.r;", "p.r = -(-p.a);")).containsExactly("r ASSIGN a");
    }

    @Test
    void constantStepsOnATempAreMerged() {
        assertThat(onJoin("long p.a; long p.r;", "p.r = p.a + 2 - 5;")).containsExactly(
                "t$0 ASSIGN a",
                "t$0 SUB_ASSIGN 3",
                "r ASSIGN t$0");
    }

    @Test
    void returnValuesAreBuiltInTheReturnSlot() {
        assertThat(lowered("long p.a; long p.r;\n" + SQUARE_PLUS_ONE + "event onJoin { p.r = sq(p.a); }", "sq")).containsExactly(
                "o$ ASSIGN sq_p$0",
                "o$ MUL_ASSIGN sq_p$0",
                "o$ ADD_ASSIGN 1",
                "RETURN");
    }

    @Test
    void argumentsAreBuiltInTheParameterSlot() {
        assertThat(onJoin("long p.a; long p.r;\n" + SQUARE_PLUS_ONE, "p.r = sq(p.a + 1);")).containsExactly(
                "sq_p$0 ASSIGN a",
                "sq_p$0 ADD_ASSIGN 1",
                "CALL sq",
                "r ASSIGN o$");
    }

    @Test
    void aCallResultIsUsedInPlace() {
        assertThat(onJoin("long p.a; long p.r;\n" + SQUARE_PLUS_ONE, "p.r = sq(p.a) + 1;")).containsExactly(
                "sq_p$0 ASSIGN a",
                "CALL sq",
                "o$ ADD_ASSIGN 1",
                "r ASSIGN o$");
    }

    @Test
    void aCallResultNeededAfterAnotherCallStaysInATemp() {
        List<String> body = onJoin("long p.a; long p.b; long p.r;\n" + SQUARE_PLUS_ONE, "p.r = sq(p.a) + sq(p.b);");

        int secondCall = body.lastIndexOf(body.stream().filter(l -> l.startsWith("CALL")).toList().getLast());
        assertThat(body.subList(0, secondCall)).contains("t$0 ASSIGN o$");
        assertThat(body.subList(secondCall, body.size())).contains("t$0 ADD_ASSIGN o$");
    }

    @Test
    void comparisonWithAConstantSkipsTheTemp() {
        assertThat(onJoin("long p.a; long p.r;", "if (p.a + 1 > 5) { p.r = 1; }")).containsExactly(
                "IF [a GREATER_THAN 4] THEN [r ASSIGN 1]");
        assertThat(onJoin("long p.a; long p.r;", "if (p.a - 3 <= 10) { p.r = 1; }")).containsExactly(
                "IF [a LESS_THAN_EQUALS 13] THEN [r ASSIGN 1]");
    }

    @Test
    void aWriteAfterTheIfDoesNotBlockTheRewrite() {
        // The variable is read when the condition runs, before the write that follows the if.
        List<String> body = onJoin("long p.a; long p.r;", "if (p.a + 1 > 5) { p.r = 1; }\np.a = 2;");
        assertThat(body.getFirst()).startsWith("IF [a GREATER_THAN 4]");
        assertThat(body.getLast()).isEqualTo("a ASSIGN 2");
    }

    @Test
    void doubleStepsAreNotCombined() {
        assertThat(onJoin("double p.a; double p.r;", "p.r = p.a + 0.1 + 0.2;")).containsExactly(
                "t$0 ASSIGN a",
                "t$0 ADD_ASSIGN 0.1",
                "t$0 ADD_ASSIGN 0.2",
                "r ASSIGN t$0");
    }
}
