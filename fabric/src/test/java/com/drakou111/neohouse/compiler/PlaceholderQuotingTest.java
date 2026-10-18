package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Team names and fallbacks with spaces are quoted inside %var...% placeholders, so Housing keeps them whole. */
class PlaceholderQuotingTest {

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
    void aFallbackWithASpaceIsQuoted() {
        assertThat(steps("string p.s;\nevent onJoin { message(\"hi \" + p.s?\"a b\"); }"))
                .contains("Enter text: hi %var.player/s \"a b\"%");
    }

    @Test
    void aTeamNameWithASpaceIsQuoted() {
        assertThat(steps("event onJoin { t[\"red team\"].x = 1; message(\"\" + t[\"red team\"].x?0); }"))
                .contains("Enter text: %var.team/x \"red team\" 0%");
    }

    @Test
    void singleWordsStayAsTheyAre() {
        assertThat(steps("event onJoin { t[red].x = 1; message(\"\" + t[red].x?5); }"))
                .contains("Enter text: %var.team/x red 5%");
    }

    @Test
    void aConditionsFallbackFieldIsNotQuoted() {
        assertThat(steps("string p.s;\nevent onJoin { if (p.s?\"a b\" == \"x\") { p.y = 1; } }"))
                .contains("Enter text: a b");
    }
}
