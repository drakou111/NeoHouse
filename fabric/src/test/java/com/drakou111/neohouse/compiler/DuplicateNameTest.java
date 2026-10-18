package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplicateNameTest {

    private static void assertRejected(String source, String message) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining(message);
    }

    @Test
    void twoFunctionsWithTheSameName() {
        assertRejected("function a() { p.x = 1; }\nfunction a() { p.x = 2; }", "There's already a function named 'a' (line 1)");
    }

    @Test
    void aLoopAndAFunctionWithTheSameName() {
        assertRejected("function tick() { p.x = 1; }\nloop tick(20) { p.x = 2; }", "Loops are functions in Housing");
    }

    @Test
    void twoCommandsWithTheSameName() {
        assertRejected("command shop { p.x = 1; }\ncommand shop { p.x = 2; }", "There's already a command named 'shop'");
    }

    @Test
    void twoMenusWithTheSameName() {
        assertRejected("menu shop(1) { }\nmenu \"shop\"(2) { }", "There's already a menu named 'shop'");
    }

    @Test
    void aFunctionCommandAndMenuMayShareAName() {
        assertThatCode(() -> TestCompiler.analyze("function shop() { p.x = 1; }\ncommand shop { shop(); }\nmenu shop(1) { }"))
                .doesNotThrowAnyException();
    }
}
