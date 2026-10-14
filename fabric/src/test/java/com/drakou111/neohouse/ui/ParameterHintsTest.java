package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.ui.ParameterHints.CallAt;
import com.drakou111.neohouse.ui.ParameterHints.InlineHint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterHintsTest {

    private static final String DECLARATION = "function f(long a, string b) {";

    private static ParameterHints hintsFor(StringBuilder source) {
        return new ParameterHints(() -> source);
    }

    private static List<InlineHint> lineHints(String source, int lineIndex, int cursorColumn) {
        ParameterHints hints = hintsFor(new StringBuilder(source));
        return hints.forLine(List.of(source.split("\n", -1)), lineIndex, cursorColumn, CallAt.NONE);
    }

    @Test
    void userFunctionLabelsComeFromItsDeclaration() {
        ParameterHints hints = hintsFor(new StringBuilder(DECLARATION + "}"));

        assertThat(hints.parameterLabels("f")).containsExactly("a: long", "b: string");
        assertThat(hints.parameterLabels("unknown")).isEmpty();
    }

    @Test
    void callArgumentsAreLabelled() {
        List<InlineHint> hints = lineHints(DECLARATION + "}\nf(1, \"x\")", 1, -1);

        assertThat(hints).containsExactly(new InlineHint(2, "a: ", false), new InlineHint(5, "b: ", false));
    }

    @Test
    void theDeclarationItselfIsNotLabelled() {
        assertThat(lineHints(DECLARATION + "}", 0, -1)).isEmpty();
    }

    @Test
    void activeArgumentIsMarked() {
        ParameterHints hints = hintsFor(new StringBuilder(DECLARATION + "}\nf(1, 2)"));
        List<InlineHint> line = hints.forLine(List.of(DECLARATION + "}", "f(1, 2)"), 1, -1, new CallAt("f", 1));

        assertThat(line).extracting(InlineHint::isActive).containsExactly(false, true);
    }

    @Test
    void emptyArgumentAtTheEndOfTheLineIsLabelledWhileTyping() {
        String call = "f(1, ";
        List<InlineHint> hints = lineHints(DECLARATION + "}\n" + call, 1, call.length());

        assertThat(hints).containsExactly(new InlineHint(2, "a: ", false), new InlineHint(5, "b: ", false));
    }

    @Test
    void indentedContinuationLineIsLabelled() {
        String source = DECLARATION + "}\nf(\n    1,\n    \"x\")";

        assertThat(lineHints(source, 2, -1)).containsExactly(new InlineHint(4, "a: ", false));
        assertThat(lineHints(source, 3, -1)).containsExactly(new InlineHint(4, "b: ", false));
    }

    @Test
    void parsedFunctionsAreRefreshedAfterInvalidate() {
        StringBuilder source = new StringBuilder("function f(long a) {}");
        ParameterHints hints = hintsFor(source);
        assertThat(hints.parameterLabels("f")).containsExactly("a: long");

        source.setLength(0);
        source.append("function f(double z) {}");
        assertThat(hints.parameterLabels("f")).containsExactly("a: long");
        hints.invalidate();
        assertThat(hints.parameterLabels("f")).containsExactly("z: double");
    }

    @Test
    void callAtFindsTheEnclosingCallAndArgument() {
        assertThat(ParameterHints.callAt("foo(1, bar(2, 3), ")).contains(new CallAt("foo", 2));
        assertThat(ParameterHints.callAt("foo(\"a, b\", ")).contains(new CallAt("foo", 1));
        assertThat(ParameterHints.callAt("my_func(")).contains(new CallAt("my_func", 0));
        assertThat(ParameterHints.callAt("x = 1")).isEmpty();
    }
}
