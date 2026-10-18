package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariableHolderTest {

    private static void assertNeedsHolder(String source) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Variable 'test' needs a holder: write p.test");
    }

    @Test
    void aTopLevelDeclarationNeedsAHolder() {
        assertNeedsHolder("long test;");
    }

    @Test
    void aDeclarationInsideAFunctionNeedsAHolder() {
        assertNeedsHolder("function f() { long test; p.y = 1; }");
    }

    @Test
    void anEnumTypedDeclarationNeedsAHolder() {
        assertNeedsHolder("enum Color { RED, BLUE }\nColor test;");
    }

    @Test
    void declarationsWithAHolderAndBareParametersAreFine() {
        assertThatCode(() -> TestCompiler.analyze("""
                long p.test;
                double g.speed;
                string t[red].name;
                function f(long amount) { p.test = amount; }
                """)).doesNotThrowAnyException();
    }
}
