package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionLimitTest {

    private static String conditional(String condition, int count, String joiner) {
        return "function f() { if (" + String.join(joiner, Collections.nCopies(count, condition)) + ") { p.y = 1; } }";
    }

    @Test
    void twentyConditionsOfOneTypeAreAllowed() {
        assertThatCode(() -> TestCompiler.analyze(conditional("p.x == 10", 20, " || "))).doesNotThrowAnyException();
    }

    @Test
    void twentyOneVariableConditionsAreRejected() {
        assertThatThrownBy(() -> TestCompiler.analyze(conditional("p.x == 10", 21, " || ")))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("at most 20");
    }

    @Test
    void limitIsPerConditionType() {
        String mixed = "function f() { if (" + String.join(" && ", Collections.nCopies(20, "p.x == 10")) + " && "
                + String.join(" && ", Collections.nCopies(20, "isSneaking(false)")) + ") { p.y = 1; } }";

        assertThatCode(() -> TestCompiler.analyze(mixed)).doesNotThrowAnyException();
    }

    @Test
    void twentyOneSneakingConditionsAreRejected() {
        assertThatThrownBy(() -> TestCompiler.analyze(conditional("isSneaking(false)", 21, " && ")))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("at most 20");
    }
}
