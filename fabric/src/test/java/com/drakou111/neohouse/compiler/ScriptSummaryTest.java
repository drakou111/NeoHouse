package com.drakou111.neohouse.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptSummaryTest {

    private static ScriptSummary summaryOf(String source) {
        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source, null, TestCompiler.DEFAULT_OPTIONS);
        return compiler.getLastSummary();
    }

    @Test
    void countsEachKindOfEntry() {
        ScriptSummary summary = summaryOf("""
                function a() { p.x = 1; }
                function b() { p.x = 2; }
                loop tick(20) { a(); }
                command shop { b(); }
                menu shop(1) { }
                menu "Other"(1) { }
                event onJoin { a(); }
                """);

        assertThat(summary).isEqualTo(new ScriptSummary(2, 2, 1, 1, 2, 1));
        assertThat(summary.describe()).isEqualTo("2 functions, 1 loop, 1 command, 2 menus, 1 event");
    }

    @Test
    void functionsSplitToFitHousingLimitsShowTheCompiledCount() {
        StringBuilder body = new StringBuilder();
        // More messages than one function's 25 conditionals can hold.
        for (int i = 0; i < 600; i++) body.append("message(\"").append(i).append("\");\n");

        ScriptSummary summary = summaryOf("function big() {\n" + body + "}");

        assertThat(summary.functions()).isEqualTo(1);
        assertThat(summary.compiledFunctions()).isGreaterThan(1);
        assertThat(summary.describe()).startsWith("1 function (" + summary.compiledFunctions() + " once compiled)");
    }

    @Test
    void anEmptyScriptHasNothing() {
        assertThat(summaryOf("").describe()).isEqualTo("nothing");
    }
}
