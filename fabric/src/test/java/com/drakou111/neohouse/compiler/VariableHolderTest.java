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

    private static void assertUnknownName(String source) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Unknown name 'test': it isn't a constant or a parameter");
    }

    @Test
    void assigningABareNameIsAnError() {
        assertUnknownName("event onJoin { test = 5; }");
        assertUnknownName("event onJoin { test++; }");
    }

    @Test
    void readingABareNameIsAnErrorEvenWhenThePlayerVariableExists() {
        assertUnknownName("long p.test;\nevent onJoin { p.y = test + 1; }");
    }

    @Test
    void aBareNameIsOnlyAParameterInsideItsOwnFunction() {
        assertUnknownName("function f(long test) { p.y = test; }\nfunction g() { p.y = test; }");
    }

    @Test
    void aBareTupleTargetIsAnError() {
        assertUnknownName("""
                function (long, long) pair() { return (1, 2); }
                event onJoin { (test, p.b) = pair(); }
                """);
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
