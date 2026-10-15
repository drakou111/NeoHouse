package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.config.UnsetVariableMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The setting for reads of variables the script never sets. */
class UnsetVariableCheckTest {

    private static List<String> warningsOf(String source, UnsetVariableMode mode) {
        CompileOptions options = new CompileOptions(MultiCallMode.FIX, RecursionMode.WARN, GlobalCallMode.ERROR,
                OverrideMode.IF_EMPTY, UndeclaredVariableMode.ALLOW, false, mode);
        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source, null, options);
        return compiler.getLastWarnings().stream().map(CompileWarning::message).toList();
    }

    private static List<String> warningsOf(String source) {
        return warningsOf(source, UnsetVariableMode.WARN);
    }

    @Test
    void aConditionOnANeverSetVariableIsReported() {
        assertThat(warningsOf("long p.x; long p.y;\nfunction f() { if (p.x == 1) { p.y = 1; } }"))
                .singleElement()
                .satisfies(message -> assertThat(message)
                        .contains("'p.x' is read but never set")
                        .contains("p.x?0"));
    }

    @Test
    void theVariableIsFoundOnEitherSideOfTheComparison() {
        assertThat(warningsOf("long p.x; long p.y;\nfunction f() { if (3 < p.x) { p.y = 1; } }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.x' is read but never set"));
    }

    @Test
    void aBoolUsedAsTheConditionIsReported() {
        assertThat(warningsOf("bool p.vip; long p.y;\nfunction f() { if (p.vip) { p.y = 1; } }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.vip' is read but never set"));
    }

    @Test
    void aReadInACalculationIsReported() {
        assertThat(warningsOf("long p.x; long p.y;\nfunction f() { p.y = p.x + 1; }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.x' is read but never set"));
    }

    @Test
    void theComparedValueIsAReadToo() {
        assertThat(warningsOf("long p.x; long p.y;\nfunction f() { p.y = 2; if (p.y == p.x) { p.y = 1; } }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.x' is read but never set"));
    }

    @Test
    void anArgumentIsAReadToo() {
        assertThat(warningsOf("double p.hp;\nfunction f() { setHealth(p.hp, \"SET\"); }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.hp' is read but never set"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"p.x += 1;", "p.x -= 2;", "p.x *= 3;", "p.x++;", "p.x--;"})
    void aCompoundAssignmentReadsTheVariable(String statement) {
        assertThat(warningsOf("long p.x;\nfunction f() { " + statement + " }"))
                .singleElement()
                .satisfies(message -> assertThat(message).contains("'p.x' is read but never set"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // a compound assignment after the variable is set
            "long p.x;\nfunction f() { p.x = 0; p.x += 1; }",
            // a fallback on a read in a calculation
            "long p.x; long p.y;\nfunction f() { p.y = p.x?0 + 1; }",
            // set later or elsewhere in the script
            "long p.x; long p.y;\nfunction f() { if (p.x == 1) { p.y = 1; } }\nfunction g() { p.x = 1; }",
            // a fallback covers the unset case
            "long p.x; long p.y;\nfunction f() { if (p.x?0 == 1) { p.y = 1; } }",
            // a starting value counts as set
            "long g.x = 5; long p.y;\nfunction f() { if (g.x == 1) { p.y = 1; } }",
            // parameters and return values are always set by the caller and the function
            "long p.y;\nfunction f(long n) { if (n == 1) { p.y = 1; } }\nevent onJoin { f(2); }",
            "long p.y;\nfunction long sq(long n) { return n * n; }\nevent onJoin { if (sq(2) == 4) { p.y = 1; } }"
    })
    void setOrCoveredVariablesAreNotReported(String source) {
        assertThat(warningsOf(source)).isEmpty();
    }

    @Test
    void eachVariableIsReportedOnce() {
        String source = "long p.x; long p.y;\nfunction f() { if (p.x == 1) { p.y = 1; } if (p.x > 2) { p.y = 2; } }";

        assertThat(warningsOf(source)).hasSize(1);
    }

    @Test
    void canBeAnError() {
        assertThatThrownBy(() -> warningsOf("long p.x; long p.y;\nfunction f() { if (p.x == 1) { p.y = 1; } }", UnsetVariableMode.ERROR))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("'p.x' is read but never set");
    }

    @Test
    void canBeTurnedOff() {
        assertThat(warningsOf("long p.x; long p.y;\nfunction f() { if (p.x == 1) { p.y = 1; } }", UnsetVariableMode.OFF)).isEmpty();
    }
}
