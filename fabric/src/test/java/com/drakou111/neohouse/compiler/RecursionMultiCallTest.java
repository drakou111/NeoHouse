package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Recursion follows the Recursion setting, even though a recursive call also runs a function twice. */
class RecursionMultiCallTest {

    private static List<String> warningsOf(String source, MultiCallMode multiCall, RecursionMode recursion) {
        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source, null, new CompileOptions(multiCall, recursion, GlobalCallMode.ERROR, OverrideMode.ALWAYS));
        return compiler.getLastWarnings().stream().map(CompileWarning::message).toList();
    }

    @Test
    void selfRecursionFollowsTheRecursionSetting() {
        assertThat(warningsOf("function f() { f(); }", MultiCallMode.ERROR, RecursionMode.WARN))
                .singleElement().asString().startsWith("Recursive call: f");
        assertThatThrownBy(() -> warningsOf("function f() { f(); }", MultiCallMode.ERROR, RecursionMode.ERROR))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Recursive call");
    }

    @Test
    void mutualRecursionFollowsTheRecursionSetting() {
        assertThat(warningsOf("function f() { g(); }\nfunction g() { f(); }", MultiCallMode.ERROR, RecursionMode.WARN))
                .allSatisfy(message -> assertThat(message).startsWith("Recursive call"));
    }

    @Test
    void fixModeDoesntCopyARecursiveFunctionForever() {
        assertThat(warningsOf("function f() { f(); }", MultiCallMode.FIX, RecursionMode.OFF)).isEmpty();
    }

    @Test
    void callingTheSameFunctionTwiceIsStillAMultiCall() {
        assertThatThrownBy(() -> warningsOf("function f() { p.x = 1; }\nevent onJoin { f(); f(); }", MultiCallMode.ERROR, RecursionMode.WARN))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("would run more than once");
    }

    @Test
    void whatARecursiveFunctionRunsStillCountsForItsCallers() {
        // f already runs g, so calling g again from the event is a multi-call.
        assertThatThrownBy(() -> warningsOf("function f() { g(); }\nfunction g() { f(); }\nevent onJoin { f(); g(); }",
                MultiCallMode.ERROR, RecursionMode.OFF))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Function 'g' would run more than once");
    }
}
