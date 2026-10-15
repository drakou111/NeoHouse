package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.constants.conditions.VariableInputs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionFallbackTest {
    private static final String FALLBACK_FIELD = "Search: " + VariableInputs.FALLBACK_VALUE;

    private static List<String> names(String body) {
        return names("", body);
    }

    private static List<String> names(String declarations, String body) {
        List<String> names = new ArrayList<>();
        String source = declarations + "\nfunction f() { " + body + " }";
        collect(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(source)), names);
        return names;
    }

    private static void collect(Action action, List<String> into) {
        into.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, into);
    }

    /** The text typed right after opening the Fallback Value field, or null when it isn't set. */
    private static String fallbackOf(String body) {
        return fallbackOf("", body);
    }

    private static String fallbackOf(String declarations, String body) {
        List<String> names = names(declarations, body);
        int field = names.indexOf(FALLBACK_FIELD);
        if (field < 0) return null;
        for (int i = field + 1; i < names.size(); i++)
            if (names.get(i).startsWith("Enter text: ")) return names.get(i).substring("Enter text: ".length());
        return null;
    }

    @Test
    void aFallbackOnTheComparedVariableFillsTheFallbackValueField() {
        assertThat(fallbackOf("if (p.name?5 == 3) { p.y = 1; }")).isEqualTo("5");
    }

    @Test
    void theFallbackFollowsTheVariableWhenSidesAreSwapped() {
        assertThat(fallbackOf("if (3 < p.name?5) { p.y = 1; }")).isEqualTo("5");
    }

    @Test
    void stringAndNegativeFallbacksAreKeptAsWritten() {
        assertThat(fallbackOf("if (p.rank?\"none\" == \"vip\") { p.y = 1; }")).isEqualTo("none");
        assertThat(fallbackOf("if (p.name?-2 > 0) { p.y = 1; }")).isEqualTo("-2");
    }

    @Test
    void aFallbackOnTheRightSideStaysInsideItsPlaceholder() {
        List<String> names = names("if (p.name == p.test?0) { p.y = 1; }");

        assertThat(names).doesNotContain(FALLBACK_FIELD);
        assertThat(names).contains("Enter text: %var.player/test 0%");
    }

    @Test
    void noFallbackLeavesTheFieldAlone() {
        assertThat(names("if (p.name == 3) { p.y = 1; }")).doesNotContain(FALLBACK_FIELD);
    }

    @Test
    void aFallbackInsideAnExpressionIsKeptInItsPlaceholder() {
        assertThat(names("if (p.name?5 + 1 == 3) { p.y = 1; }")).anyMatch(name -> name.contains("%var.player/name 5%"));
    }

    @Test
    void aBoolVariableUsedAsTheConditionKeepsItsFallback() {
        assertThat(fallbackOf("bool p.flag;", "if (p.flag?true) { p.y = 1; }")).isEqualTo("1");
        assertThat(fallbackOf("bool p.flag;", "if (!p.flag?false) { p.y = 1; }")).isEqualTo("0");
    }
}
