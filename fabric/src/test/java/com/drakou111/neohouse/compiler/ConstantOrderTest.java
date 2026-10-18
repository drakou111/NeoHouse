package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Constants and enums can be used anywhere in the file, also above their declaration. */
class ConstantOrderTest {

    private static void assertCompiles(String source) {
        assertThatCode(() -> TestCompiler.analyze(source)).doesNotThrowAnyException();
    }

    private static void assertRejected(String source, String message) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining(message);
    }

    @Test
    void aConstantCanBeUsedAboveItsDeclaration() {
        assertCompiles("function f() { p.y = MAX; }\nconst long MAX = 5;");
    }

    @Test
    void aConstantCanUseOneDeclaredFurtherDown() {
        assertCompiles("const long DOUBLE_MAX = MAX;\nconst long MAX = 5;\nfunction f() { p.y = DOUBLE_MAX; }");
    }

    @Test
    void anEnumCanBeUsedAboveItsDeclaration() {
        assertCompiles("Color p.c;\nfunction f() { p.c = Color.RED; if (p.c == BLUE) { p.y = 1; } }\nenum Color { RED, BLUE }");
    }

    @Test
    void aParameterNamedLikeALaterConstantIsStillRejected() {
        assertRejected("function f(long MAX) { p.y = MAX; }\nconst long MAX = 5;", "'MAX' is already a constant");
    }

    @Test
    void constantsThatOnlyReferToEachOtherAreAnError() {
        assertRejected("const long A = B;\nconst long B = A;", "Unknown name");
    }

    @Test
    void aDuplicateConstantIsStillAnError() {
        assertRejected("const long X = 1;\nconst long X = 2;", "Constant 'X' is already declared");
    }
}
