package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NestingMessageTest {

    private static void assertRejected(String source, String message) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining(message);
    }

    @Test
    void elseIfExplainsWhyItIsntSupported() {
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if (p.a == 1) { p.y = 1; } else if (p.a == 2) { p.y = 2; } }",
                "'else if' isn't supported: Housing can't put a conditional inside another one");
    }

    @Test
    void notAroundAComparisonSuggestsTheOppositeComparison() {
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if (!(p.a == 1)) { p.y = 1; } }",
                "'!(...)' isn't supported around a condition: write the opposite comparison instead");
    }

    @Test
    void bracketsAroundConditionsExplainTheFlatList() {
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if (p.a == 1 && (p.a == 2 || p.a == 3)) { p.y = 1; } }",
                "Brackets around conditions aren't supported");
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if ((p.a == 2)) { p.y = 1; } }",
                "Brackets around conditions aren't supported");
    }

    @Test
    void bracketsInArithmeticAndNotOnABoolStillWork() {
        assertThatCode(() -> TestCompiler.analyze(
                "long p.a; bool p.b;\nevent onJoin { p.a = 1; p.b = true; if ((p.a + 1) * 2 > 5 && !p.b) { p.y = 1; } }"))
                .doesNotThrowAnyException();
    }

    @Test
    void anIfInsideAnIfExplainsWhatToDo() {
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if (p.a == 1) { if (p.a == 2) { p.y = 1; } } }",
                "An if can't go inside an if or a random block");
    }
}
