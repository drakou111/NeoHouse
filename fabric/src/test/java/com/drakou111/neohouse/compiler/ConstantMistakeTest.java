package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstantMistakeTest {

    private static void assertRejected(String source, String message) {
        assertThatThrownBy(() -> TestCompiler.analyze(source))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining(message);
    }

    private static List<String> warningsOf(String source) {
        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source, null, TestCompiler.DEFAULT_OPTIONS);
        return compiler.getLastWarnings().stream().map(CompileWarning::message).toList();
    }

    @Test
    void dividingByAConstantZeroIsAnError() {
        assertRejected("event onJoin { p.x = 5 / 0; }", "Dividing by zero");
        assertRejected("event onJoin { p.x = p.y?1 / 0.0; }", "Dividing by zero");
        assertRejected("event onJoin { p.x /= 0; }", "Dividing by zero");
    }

    @Test
    void aRemainderByAConstantZeroIsAnError() {
        assertRejected("event onJoin { p.x = 5 % 0; }", "remainder of a division by zero");
        assertRejected("event onJoin { p.x %= -0; }", "remainder of a division by zero");
    }

    @Test
    void aZeroConstantCountsToo() {
        assertRejected("const long NONE = 0;\nevent onJoin { p.x = 5 / NONE; }", "Dividing by zero");
    }

    @Test
    void dividingByAVariableIsLeftToHousing() {
        assertThatCode(() -> TestCompiler.analyze("event onJoin { p.x = 5 / p.y?1; }")).doesNotThrowAnyException();
    }

    @Test
    void repeatZeroWarns() {
        assertThat(warningsOf("event onJoin { repeat(0) { p.x = 1; } }")).contains("repeat(0) never runs its body.");
        assertThat(warningsOf("event onJoin { repeat(2) { p.x = 1; } }")).doesNotContain("repeat(0) never runs its body.");
    }
}
