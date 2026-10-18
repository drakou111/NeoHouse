package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstantShadowingTest {

    private static void assertRejected(String source, String message) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining(message);
    }

    @Test
    void aParameterNamedLikeAConstantIsRejected() {
        assertRejected("""
                const long x = 1;

                // Returns the absolute value of x.
                function long long_abs(long x) {
                    if (x < 0) { return -x; }
                    return x;
                }
                """, "'x' is already a constant (line 1)");
    }

    @Test
    void theOrderDoesntMatter() {
        assertRejected("function f(long x) { p.y = x; }\nconst long x = 1;", "'x' is already a constant (line 2)");
    }

    @Test
    void aParameterNamedLikeAnEnumMemberIsRejected() {
        assertRejected("enum Color { RED, BLUE }\nfunction f(long RED) { p.y = 1; }", "member of enum 'Color'");
    }

    @Test
    void aHolderPrefixedVariableMayShareTheName() {
        assertThatCode(() -> TestCompiler.analyze("const long x = 1;\nfunction f() { p.x = x; }"))
                .doesNotThrowAnyException();
    }
}
