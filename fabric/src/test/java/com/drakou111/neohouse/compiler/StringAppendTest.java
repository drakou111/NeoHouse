package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringAppendTest {

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
    void appendingToAStringSetsItToTheJoinedText() {
        List<String> steps = steps("string p.s;\nevent onJoin { p.s = \"a\"; p.s += \"x\"; }");

        assertThat(steps).contains("Enter text: %var.player/s%x");
        assertThat(steps.stream().filter(step -> step.equals("Search: Set")).count()).isEqualTo(2);
    }

    @Test
    void appendingANumberJoinsItAsText() {
        assertThat(steps("string p.s;\nevent onJoin { p.s = \"a\"; p.s += 1; }")).contains("Enter text: %var.player/s%1");
    }

    @Test
    void settingEmptyTextTypesTwoQuotes() {
        assertThat(steps("string p.s;\nevent onJoin { p.s = \"\"; }")).contains("Enter text: \"\"");
    }

    @Test
    void numbersStillIncrement() {
        List<String> steps = steps("long p.n;\nevent onJoin { p.n = 1; p.n += 2; }");

        assertThat(steps).doesNotContain("Enter text: %var.player/n%2");
        assertThat(steps.stream().filter(step -> step.equals("Search: Set")).count()).isEqualTo(1);
    }
}
