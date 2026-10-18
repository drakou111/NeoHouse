package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Text keeps reading a parameter or temp after the compiler renames it. */
class RenamedInTextTest {

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
    void aParameterInTextReadsItsSlot() {
        assertThat(steps("function hi(string who) { message(\"hi \" + who); }\nevent onJoin { hi(\"Bob\"); }"))
                .contains("Enter text: hi %var.player/hi_p$0%")
                .doesNotContain("Enter text: hi %var.player/who%");
    }

    @Test
    void aReturnedTextReadsTheParameterSlot() {
        assertThat(steps("function string greet(string n) { return \"hi \" + n; }\nevent onJoin { message(greet(\"Al\")); }"))
                .contains("Enter text: hi %var.player/greet_p$0%");
    }

    @Test
    void aTempInTextInsideAFunctionReadsItsRenamedTemp() {
        List<String> steps = steps("long p.a;\nfunction show() { message(\"next \" + (p.a * 2)); }\nevent onJoin { p.a = 1; show(); }");

        assertThat(steps).containsSubsequence("Enter text: show$t0", "Enter text: next %var.player/show$t0%")
                .doesNotContain("Enter text: next %var.player/t$0%");
    }
}
