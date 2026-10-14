package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.compiler.visitors.CompilerNames;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FunctionTempNamingTest {

    private static final Pattern VARIABLE = Pattern.compile("name=([^,\\]]+)");
    // Needs a temp of its own besides the return slot.
    private static final String CALLEE = "function long sq(long x) { return (x + 1) * (x + 2); }\n";

    private static MainStatementNode body(MainNode main, String name) {
        for (MainStatementNode statement : main.statements) {
            if (statement instanceof FunctionNode f && f.name.equals(name)) return statement;
            if (statement instanceof EventNode e && e.event.name.equals(name)) return statement;
        }
        throw new AssertionError("no body named " + name);
    }

    /** Names of the compiler-made variables (containing '$') in a body. */
    private static Set<String> compilerVariablesOf(MainNode main, String name) {
        Set<String> names = new TreeSet<>();
        Matcher m = VARIABLE.matcher(body(main, name).toString());
        while (m.find()) if (m.group(1).contains("$")) names.add(m.group(1));
        return names;
    }

    @Test
    void eachFunctionHasItsOwnTemps() {
        MainNode main = TestCompiler.analyze("long p.a; long p.b; long p.r;\n" + CALLEE
                + "function f() { p.r = p.a * 2 + sq(p.b); }");

        assertThat(compilerVariablesOf(main, "f")).contains("f$t0").noneMatch(n -> n.startsWith("sq$t"));
        assertThat(compilerVariablesOf(main, "sq")).contains("sq$t1").noneMatch(n -> n.startsWith("f$t"));
    }

    @Test
    void entryPointsKeepPlainTemps() {
        MainNode main = TestCompiler.analyze("long p.a; long p.r;\n" + CALLEE
                + "event onJoin { p.r = p.a * 2 + sq(p.a); }");

        assertThat(compilerVariablesOf(main, "onJoin")).contains("t$0");
    }

    @Test
    void aFunctionCompilesTheSameWhateverElseChanges() {
        String withOneCaller = "long p.a; long p.r;\n" + CALLEE + "function f() { p.r = sq(p.a); }";
        String withOtherFunctions = "long p.a; long p.r;\n" + CALLEE
                + "function long twice(long y) { return y * 2 + sq(y) * 3; }\n"
                + "function f() { p.r = p.a * (p.a + 4) + twice(p.a); }";

        String alone = body(TestCompiler.analyze(withOneCaller), "sq").toString();
        String withOthers = body(TestCompiler.analyze(withOtherFunctions), "sq").toString();
        assertThat(withOthers).isEqualTo(alone);
    }

    @Test
    void longFunctionNamesUseTheirCode() {
        MainNode main = TestCompiler.analyze("long p.r;\n"
                + "function long calculateTotal(long x) { return (x + 1) * (x + 2); }\n"
                + "function f() { p.r = calculateTotal(2); }");

        assertThat(compilerVariablesOf(main, "calculateTotal")).contains("$jj6a7rp0", "$jj6a7rt1");
        assertThat(compilerVariablesOf(main, "f")).contains("$jj6a7rp0");
    }

    @Test
    void codesAreStable() {
        // Changing the code would rename existing variables in every house using long function names.
        assertThat(CompilerNames.code("calculateTotal")).isEqualTo("jj6a7r");
        assertThat(CompilerNames.code("getFacingVector")).isEqualTo("xv3952");
        assertThat(CompilerNames.parameterSlot("sq", 0)).isEqualTo("sq_p$0");
        assertThat(CompilerNames.parameterSlot("averageScore", 1)).isEqualTo("averageScore_p$1");
        assertThat(CompilerNames.temp("sq$2", 3)).isEqualTo("sq$t3");
    }

    @Test
    void longFunctionNamesWithParametersCompile() {
        assertThatCode(() -> TestCompiler.analyze("long p.r;\n"
                + "function long aVeryLongFunctionNameIndeed(long x, long y) { return x + y; }\n"
                + "event onJoin { p.r = aVeryLongFunctionNameIndeed(1, 2); }")).doesNotThrowAnyException();
    }
}
