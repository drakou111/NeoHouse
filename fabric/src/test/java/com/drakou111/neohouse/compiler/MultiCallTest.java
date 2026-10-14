package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.expr.ExpressionNode;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementListNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.compiler.ast.statement.VoidFunctionCallNode;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiCallTest {
    @TempDir
    Path scripts;

    private static CompileOptions options(MultiCallMode mode) {
        return new CompileOptions(mode, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.ALWAYS);
    }

    private MainNode compileWithTrig(String body, MultiCallMode mode) throws IOException {
        Path library = this.scripts.resolve("neohouse/trig.nh");
        Files.createDirectories(library.getParent());
        try (InputStream in = getClass().getResourceAsStream("/assets/neohouse/library/neohouse/trig.nh")) {
            Files.write(library, in.readAllBytes());
        }
        Path main = this.scripts.resolve("main.nh");
        Files.writeString(main, "import \"neohouse/trig.nh\";\ndouble p.a;\nfunction f() {\n" + body + "\n}\n");
        return TestCompiler.analyze(Files.readString(main), main, options(mode));
    }

    /** Function name -> names it triggers (after lowering every call is a trigger). */
    private static Map<String, List<String>> callGraph(MainNode main) {
        Map<String, List<String>> graph = new HashMap<>();
        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode function) {
                List<String> calls = new ArrayList<>();
                collectCalls(function.statements, calls);
                graph.put(function.name, calls);
            }
        }
        return graph;
    }

    private static void collectCalls(StatementListNode body, List<String> calls) {
        if (body == null) return;
        for (StatementNode statement : body.statements) {
            if (statement instanceof VoidFunctionCallNode call) calls.add(call.functionName);
            if (statement instanceof ConditionalNode conditional) {
                collectCalls(conditional.thenBlock, calls);
                collectCalls(conditional.elseBlock, calls);
            }
        }
    }

    /** Every function that one run of {@code root} can reach, counting each time it's triggered. */
    private static List<String> reachable(Map<String, List<String>> graph, String root) {
        List<String> reached = new ArrayList<>();
        for (String callee : graph.getOrDefault(root, List.of())) {
            reached.add(callee);
            reached.addAll(reachable(graph, callee));
        }
        return reached;
    }

    @Test
    void fixGivesEveryCallInOneRunItsOwnFunction() throws IOException {
        // cos no longer calls sin, so one run of f triggers sin twice and cos twice.
        MainNode main = compileWithTrig("p.a = sin(1.0);\np.a = sin(2.0);\np.a = cos(1.0);\np.a = cos(2.0);", MultiCallMode.FIX);

        List<String> reached = reachable(callGraph(main), "f");

        assertThat(reached).hasSize(4);
        assertThat(new LinkedHashSet<>(reached)).hasSameSizeAs(reached);
    }

    @Test
    void fixedCopiesAreAllUsed() throws IOException {
        MainNode main = compileWithTrig("p.a = sin(1.0);\np.a = sin(2.0);\np.a = cos(1.0);\np.a = cos(2.0);", MultiCallMode.FIX);
        Map<String, List<String>> graph = callGraph(main);

        Set<String> called = new LinkedHashSet<>();
        for (List<String> calls : graph.values()) called.addAll(calls);

        for (String name : graph.keySet()) {
            if (name.contains("$") && !name.contains("_s$")) assertThat(called).as("copy %s is used", name).contains(name);
        }
    }

    @Test
    void errorModeRejectsTwoCallsInOneRun() {
        assertThatThrownBy(() -> compileWithTrig("p.a = sin(1.0);\np.a = sin(2.0);", MultiCallMode.ERROR))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("'sin'");
    }

    @Test
    void errorModeAcceptsTheLibraryItself() {
        assertThatCode(() -> compileWithTrig("p.a = atan2(1.0, 2.0);", MultiCallMode.ERROR)).doesNotThrowAnyException();
    }

    @Test
    void exclusiveBranchesMayCallTheSameFunction() {
        String source = """
                function g() { p.x = 1; }
                function f() {
                    if (p.y == 1) { g(); } else { g(); }
                }
                """;

        assertThatCode(() -> TestCompiler.analyze(source, null, options(MultiCallMode.ERROR))).doesNotThrowAnyException();
    }

    @Test
    void warnModeOnlyWarns() {
        NHCompiler compiler = new NHCompiler();
        String source = """
                function g() { p.x = 1; }
                function f() { g(); g(); }
                """;

        compiler.analyze(source, null, options(MultiCallMode.WARN));

        assertThat(compiler.getLastWarnings()).anyMatch(warning -> warning.message().contains("'g'"));
    }

    @Test
    void copiesCallTheirOwnCopiesOfSharedCallees() {
        String source = """
                function g() { p.x = 1; }
                function h() { g(); }
                function f() { g(); g(); h(); h(); }
                """;

        Map<String, List<String>> graph = callGraph(TestCompiler.analyze(source, null, options(MultiCallMode.FIX)));
        List<String> reached = reachable(graph, "f");

        assertThat(reached).hasSize(6);
        assertThat(new LinkedHashSet<>(reached)).hasSameSizeAs(reached);
    }

    @Test
    void repeatedCallsInsideRepeatGetCopies() {
        String source = """
                function g() { p.x = 1; }
                function f() { repeat(3) { g(); } }
                """;

        Map<String, List<String>> graph = callGraph(TestCompiler.analyze(source, null, options(MultiCallMode.FIX)));

        assertThat(graph.get("f")).containsExactly("g", "g$0", "g$1");
    }
}
