package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.expr.constants.DoubleValue;
import com.drakou111.neohouse.config.OverrideMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Doubles reach Housing written out in full, never as 1.0E-4. */
class DoubleTextTest {

    private static List<String> steps(String source) {
        List<String> steps = new ArrayList<>();
        collect(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(source)), steps);
        return steps;
    }

    private static void collect(Action action, List<String> out) {
        out.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, out);
    }

    @Test
    void doublesAreWrittenOutInFull() {
        assertThat(new DoubleValue(0.0001)).hasToString("0.0001");
        assertThat(new DoubleValue(1.0E16)).hasToString("10000000000000000.0");
        assertThat(new DoubleValue(-12345678.5)).hasToString("-12345678.5");
        assertThat(new DoubleValue(2.0)).hasToString("2.0");
        assertThat(new DoubleValue(0.5)).hasToString("0.5");
        assertThat(new DoubleValue(0.0)).hasToString("0.0");
    }

    @Test
    void foldedValuesAndTextUseTheFullForm() {
        assertThat(steps("double p.d;\nevent onJoin { p.d = 100000000.0 * 100000000.0; }")).contains("Enter text: 10000000000000000.0");
        assertThat(steps("event onJoin { message(\"v\" + 0.0001); }")).contains("Enter text: v0.0001");
    }
}
