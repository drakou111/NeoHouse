package com.drakou111.neohouse.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest {

    @Test
    void compilesSimpleFunction() {
        String source = """
                function hello() {
                    p.x = 5;
                    p.x += 2;
                    message("hi");
                }
                """;

        assertThat(TestCompiler.analyze(source).statements).hasSize(1);
    }
}
