package com.drakou111.neohouse.compiler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class ReturnParsingTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "function long f(long x) { return (x + 1) * (x + 2); }",
            "function long f(long x) { return (x + 1) * 2; }",
            "function long f(long x) { return (x); }",
            "function long f(long x) { return x; }",
            "function (long, long) f(long x) { return (x, x + 1); }",
            "function (long, long) f(long x) { return ((x + 1) * 2, x); }"
    })
    void returnExpressionsStartingWithAParenthesis(String function) {
        assertThatCode(() -> TestCompiler.analyze(function)).doesNotThrowAnyException();
    }
}
