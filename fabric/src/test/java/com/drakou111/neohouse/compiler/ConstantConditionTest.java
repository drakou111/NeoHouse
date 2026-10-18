package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantConditionTest {

    private record Compiled(List<String> warnings, List<String> steps) {}

    private static Compiled compile(String source) {
        NHCompiler compiler = new NHCompiler();
        MainNode main = compiler.analyze(source, null, TestCompiler.DEFAULT_OPTIONS);
        List<String> steps = new ArrayList<>();
        collect(new CodeGenerator(OverrideMode.ALWAYS).generate(main), steps);
        return new Compiled(compiler.getLastWarnings().stream().map(CompileWarning::message).toList(), steps);
    }

    private static void collect(Action action, List<String> out) {
        out.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, out);
    }

    @Test
    void anAlwaysTrueConditionRunsItsActionsWithoutAConditional() {
        Compiled compiled = compile("event onJoin { if (1 == 1) { p.x = 1; } }");

        assertThat(compiled.steps()).doesNotContain("Search: Conditional").contains("Enter text: x");
        assertThat(compiled.warnings()).containsExactly("This condition is always true, so its actions always run.");
    }

    @Test
    void anAlwaysFalseConditionKeepsOnlyTheElse() {
        Compiled compiled = compile("const bool DEBUG = false;\nevent onJoin { if (DEBUG) { p.x = 1; } else { p.y = 2; } }");

        assertThat(compiled.steps()).doesNotContain("Search: Conditional", "Enter text: x").contains("Enter text: y");
        assertThat(compiled.warnings()).containsExactly("This condition is always false, so its actions never run and the else always does.");
    }

    @Test
    void combinationsAndComparisonsOfConstantsAreDecided() {
        assertThat(compile("event onJoin { if (2 > 1 && \"a\" != \"b\") { p.x = 1; } }").warnings())
                .containsExactly("This condition is always true, so its actions always run.");
        assertThat(compile("event onJoin { if (1.5 < 1.0 || false) { p.x = 1; } }").warnings())
                .containsExactly("This condition is always false, so its actions never run.");
    }

    @Test
    void aConditionOnAVariableIsKept() {
        Compiled compiled = compile("event onJoin { if (p.a?0 == 1 && 1 == 1) { p.x = 1; } }");

        assertThat(compiled.steps()).contains("Search: Conditional");
        assertThat(compiled.warnings()).isEmpty();
    }
}
