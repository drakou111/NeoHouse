package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A value computed for text is kept: the text reads it through a %var...% placeholder. */
class TextReadTest {

    private static List<String> steps(String source) {
        List<String> steps = new ArrayList<>();
        collect(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(source)), steps);
        return steps;
    }

    private static void collect(Action action, List<String> out) {
        out.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, out);
    }

    @Test
    void aSumUsedInTextIsComputedFirst() {
        List<String> steps = steps("long p.c;\nevent onJoin { p.c = 3; p.s = p.c + 1 + \"!\"; }");

        assertThat(steps).containsSubsequence(
                "Enter text: t$0", "Search: Set", "Enter text: %var.player/c%",
                "Enter text: t$0",
                "Enter text: s", "Search: Set", "Enter text: %var.player/t$0%!");
    }

    @Test
    void aSumUsedInAMessageIsComputedFirst() {
        assertThat(steps("long p.c;\nevent onJoin { p.c = 3; message(\"next: \" + (p.c * 2)); }"))
                .containsSubsequence("Enter text: t$0", "Search: Multiply", "Enter text: next: %var.player/t$0%");
    }

    @Test
    void constantMathInTextIsWrittenAsItsValue() {
        assertThat(steps("event onJoin { message(\"big \" + 100000000.0 * 100000000.0); }"))
                .contains("Enter text: big 10000000000000000.0")
                .noneMatch(step -> step.contains("t$0"));
    }
}
