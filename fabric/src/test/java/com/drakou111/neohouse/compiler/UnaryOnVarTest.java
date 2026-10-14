package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnaryOnVarTest {

    @Test
    void negatingAVarAsksForACast() {
        assertThatThrownBy(() -> TestCompiler.analyze("var p.v; double p.r;\nfunction f() { p.r = -p.v; }"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("cast it with (long) or (double)");
    }

    @Test
    void negatingACastVarCompiles() {
        assertThatCode(() -> TestCompiler.analyze("var p.v; double p.r;\nfunction f() { p.r = -(double) p.v; }"))
                .doesNotThrowAnyException();
    }
}
