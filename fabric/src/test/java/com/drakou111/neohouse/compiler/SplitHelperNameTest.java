package com.drakou111.neohouse.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SplitHelperNameTest {

    @Test
    void helpersOfALongNamedFunctionFitHousingsNameLimit() {
        String name = "f".repeat(50);
        StringBuilder body = new StringBuilder();
        // More messages than 25 conditionals can hold, so helper functions are needed.
        for (int i = 0; i < 1200; i++) body.append("message(\"").append(i).append("\");\n");

        NHCompiler compiler = new NHCompiler();
        compiler.analyze("function " + name + "() {\n" + body + "}", null, TestCompiler.DEFAULT_OPTIONS);

        assertThat(compiler.getLastHelperNames()).hasSizeGreaterThan(1)
                .allSatisfy(helper -> assertThat(helper).hasSizeLessThanOrEqualTo(50).startsWith("fff").contains("_s$"))
                .doesNotHaveDuplicates();
    }
}
