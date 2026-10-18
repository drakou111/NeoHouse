package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.NormalExpressionNode;
import com.drakou111.neohouse.compiler.ast.expr.constants.ConstantPrimitiveNode;
import com.drakou111.neohouse.compiler.ast.expr.var.VariableNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.statement.ActionCallNode;
import com.drakou111.neohouse.compiler.ast.statement.AssignationNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A call's result is used before another call in the same expression replaces it. */
class NestedCallTest {

    private static final String FUNCTIONS = """
            function long add(long a, long b) { return a + b; }
            function long sq(long a) { return a * a; }
            function long neg(long a) { return -a; }
            """;

    private static List<String> onJoin(String body) {
        MainNode main = TestCompiler.analyze(FUNCTIONS + "event onJoin { " + body + " }");
        for (MainStatementNode statement : main.statements) {
            if (!(statement instanceof EventNode event)) continue;
            List<String> lines = new ArrayList<>();
            for (StatementNode s : event.statements.statements) lines.add(render(s));
            return lines;
        }
        throw new AssertionError("no event");
    }

    private static String render(StatementNode s) {
        return switch (s) {
            case AssignationNode a -> a.variable.name + " = " + render(a.value);
            case VoidFunctionCallNode c -> "CALL " + c.functionName;
            case ActionCallNode c -> c.name + "(" + c.arguments.expressions.stream().map(NestedCallTest::render).toList() + ")";
            default -> s.getClass().getSimpleName();
        };
    }

    private static String render(NormalExpressionNode e) {
        return switch (e) {
            case VariableNode v -> v.name;
            case ConstantPrimitiveNode c -> c.value.toString();
            default -> e.getClass().getSimpleName();
        };
    }

    /** The statement index of the first line matching {@code prefix} at or after {@code from}. */
    private static int indexOf(List<String> lines, String prefix, int from) {
        for (int i = from; i < lines.size(); i++) if (lines.get(i).startsWith(prefix)) return i;
        throw new AssertionError(prefix + " not found in " + lines);
    }

    @Test
    void theFirstArgumentsResultIsTakenBeforeTheSecondCall() {
        List<String> lines = onJoin("p.x = add(sq(2), neg(3));");

        int sq = indexOf(lines, "CALL sq", 0);
        int neg = indexOf(lines, "CALL neg", sq);
        // Something takes sq's result out of o$ between the two calls.
        assertThat(lines.subList(sq + 1, neg)).anyMatch(line -> line.endsWith("= o$"));
        assertThat(lines.subList(neg + 1, lines.size())).doesNotContain("add_p$0 = o$");
    }

    @Test
    void anActionsArgumentsKeepTheirOwnResults() {
        List<String> lines = onJoin("p.x = sq(2) + neg(3); message(\"\" + sq(4) + neg(5));");

        int sq = indexOf(lines, "CALL sq", indexOf(lines, "CALL neg", 0));
        int neg = indexOf(lines, "CALL neg", sq);
        assertThat(lines.subList(sq + 1, neg)).anyMatch(line -> line.endsWith("= o$"));
    }
}
