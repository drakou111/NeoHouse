package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** '$' is kept for names the compiler makes, so a script can't collide with them. */
class ReservedNameTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "function f() { p.t$0 = 1; }",
            "function f() { g.o$ = 1; }",
            "function f() { t[red].a$b = 1; }",
            "long p.x$; function f() { }",
            "function f(long a$) { }",
            "function f$0() { }",
            "function long sq$(long x) { return x; }"
    })
    void dollarInAScriptNameIsRejected(String source) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("reserved for names the compiler makes");
    }
}
