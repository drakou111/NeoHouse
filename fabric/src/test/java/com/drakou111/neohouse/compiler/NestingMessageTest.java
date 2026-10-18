package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

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
    void anIfInsideAnIfExplainsWhatToDo() {
        assertRejected("long p.a;\nevent onJoin { p.a = 1; if (p.a == 1) { if (p.a == 2) { p.y = 1; } } }",
                "An if can't go inside an if or a random block");
    }
}
