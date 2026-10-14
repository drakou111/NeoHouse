package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.EventNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TempIsolationTest {

    private static final Pattern TEMP = Pattern.compile("name=(t\\$\\d+)");
    private static final String SQUARE_PLUS_ONE = "function long sq(long x) { return x * x + 1; }\n";

    /** The temp names used in the body of the function (or event) called {@code name}. */
    private static Set<String> tempsOf(MainNode main, String name) {
        for (MainStatementNode statement : main.statements) {
            boolean match = statement instanceof FunctionNode f && f.name.equals(name)
                    || statement instanceof EventNode e && e.event.name.equals(name);
            if (!match) continue;
            Set<String> temps = new TreeSet<>();
            Matcher m = TEMP.matcher(statement.toString());
            while (m.find()) temps.add(m.group(1));
            return temps;
        }
        throw new AssertionError("no body named " + name);
    }

    @Test
    void calleeTempsDoNotOverlapTheCallersLiveTemps() {
        MainNode main = TestCompiler.analyze("long p.a; long p.b; long p.r;\n" + SQUARE_PLUS_ONE
                + "function f() { p.r = p.a * 2 + sq(p.b); }");

        assertThat(tempsOf(main, "f")).containsExactly("t$0");
        assertThat(tempsOf(main, "sq")).doesNotContain("t$0").isNotEmpty();
    }

    @Test
    void chainsOfCallsStackTheirTemps() {
        MainNode main = TestCompiler.analyze("long p.a; long p.r;\n" + SQUARE_PLUS_ONE
                + "function long twice(long y) { return y * 2 + sq(y); }\n"
                + "function f() { p.r = p.a * 3 + twice(p.a); }");

        Set<String> f = tempsOf(main, "f");
        Set<String> twice = tempsOf(main, "twice");
        Set<String> sq = tempsOf(main, "sq");
        assertThat(twice).doesNotContainAnyElementsOf(f);
        assertThat(sq).doesNotContainAnyElementsOf(f).doesNotContainAnyElementsOf(twice);
    }

    @Test
    void entryPointsKeepTheirTempsFromZero() {
        MainNode main = TestCompiler.analyze("long p.a; long p.r;\n" + SQUARE_PLUS_ONE
                + "event onJoin { p.r = p.a * 2 + sq(p.a); }");

        assertThat(tempsOf(main, "onJoin")).containsExactly("t$0");
        assertThat(tempsOf(main, "sq")).doesNotContain("t$0");
    }

    @Test
    void functionsWithoutCallersAreUnchanged() {
        MainNode main = TestCompiler.analyze("long p.a; long p.r;\nfunction f() { p.r = p.a * 2 + 1; }");

        assertThat(tempsOf(main, "f")).containsExactly("t$0");
    }
}
