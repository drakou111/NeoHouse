package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The settings for undeclared variables and for banning the var type. */
class DeclarationRulesTest {

    private static final String UNDECLARED = "long p.coins;\nfunction f() { p.cions += 1; }";
    private static final String[] LIBRARY = {"trig.nh", "double_math.nh", "long_math.nh"};

    @TempDir
    Path scripts;

    private static CompileOptions options(UndeclaredVariableMode mode, boolean forbidVar) {
        return new CompileOptions(MultiCallMode.ERROR, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.IF_EMPTY, mode, forbidVar);
    }

    private static List<CompileWarning> warningsOf(String source, CompileOptions options) {
        NHCompiler compiler = new NHCompiler();
        compiler.analyze(source, null, options);
        return compiler.getLastWarnings();
    }

    @Test
    void undeclaredVariablesAreAllowedByDefault() {
        assertThat(warningsOf(UNDECLARED, options(UndeclaredVariableMode.ALLOW, false))).isEmpty();
    }

    @Test
    void undeclaredVariablesCanBeWarnedAbout() {
        assertThat(warningsOf(UNDECLARED, options(UndeclaredVariableMode.WARN, false)))
                .singleElement()
                .satisfies(w -> assertThat(w.message()).contains("'p.cions' is not declared"));
    }

    @Test
    void undeclaredVariablesCanBeErrors() {
        assertThatThrownBy(() -> warningsOf(UNDECLARED, options(UndeclaredVariableMode.ERROR, false)))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("'p.cions' is not declared");
    }

    @Test
    void eachUndeclaredVariableIsReportedOnce() {
        String source = "function f() { g.a = 1; g.a += 2; t[red].b = g.a; }";

        assertThat(warningsOf(source, options(UndeclaredVariableMode.WARN, false)))
                .extracting(CompileWarning::message)
                .containsExactly(
                        "'g.a' is not declared; declare it at the top of the file, e.g. long g.a;",
                        "'t[red].b' is not declared; declare it at the top of the file, e.g. long t[red].b;");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "var p.x;",
            "function f(var a) { }",
            "function var f() { return 1; }",
            "function (long, var) f() { return (1, 2); }"
    })
    void theVarTypeCanBeBanned(String source) {
        assertThatThrownBy(() -> warningsOf(source, options(UndeclaredVariableMode.ALLOW, true)))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("var type is turned off");
    }

    @Test
    void banningVarMakesUndeclaredVariablesErrors() {
        assertThatThrownBy(() -> warningsOf(UNDECLARED, options(UndeclaredVariableMode.ALLOW, true)))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("which is turned off");
    }

    @Test
    void fullyTypedScriptsCompileWithVarBanned() {
        assertThatCode(() -> warningsOf("long p.coins;\nfunction f(long n) { p.coins += n; }", options(UndeclaredVariableMode.ERROR, true)))
                .doesNotThrowAnyException();
    }

    @Test
    void theBuiltInLibraryWorksWithTheStrictestSettings() throws IOException {
        StringBuilder imports = new StringBuilder();
        for (String file : LIBRARY) {
            try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/assets/neohouse/library/neohouse/" + file))) {
                Files.copy(in, this.scripts.resolve(file));
            }
            imports.append("import \"").append(file).append("\";\n");
        }
        Path main = this.scripts.resolve("main.nh");
        Files.writeString(main, imports + "double p.r;\nlong p.n;\n"
                + "event onJoin { p.r = sin(1.0) + double_abs(-2.0) + sqrt(4.0); p.n = long_max(1, 2); }");

        assertThatCode(() -> new NHCompiler().analyze(Files.readString(main), main, options(UndeclaredVariableMode.ERROR, true)))
                .doesNotThrowAnyException();
    }
}
