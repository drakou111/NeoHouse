package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main_statements.FunctionNode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ImportTest {
    @TempDir
    Path scripts;

    private Path write(String name, String content) throws IOException {
        Path file = this.scripts.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private MainNode compile(Path file) throws IOException {
        return TestCompiler.analyze(Files.readString(file), file, TestCompiler.DEFAULT_OPTIONS);
    }

    private static List<String> functionNames(MainNode main) {
        List<String> names = new ArrayList<>();
        for (var statement : main.statements) if (statement instanceof FunctionNode function) names.add(function.name);
        return names;
    }

    @Test
    void importedFunctionIsCallable() throws IOException {
        write("lib/util.nh", "function helper() { p.x = 1; }");
        Path main = write("main.nh", """
                import "lib/util.nh";
                function run() { helper(); }
                """);

        assertThat(functionNames(compile(main))).contains("helper", "run");
    }

    @Test
    void commentedOutImportsAreIgnored() throws IOException {
        Path main = write("main.nh", """
                // import "missing.nh";
                /* import "also_missing.nh"; */
                function run() { p.x = 1; }
                """);

        assertThat(functionNames(compile(main))).containsExactly("run");
    }

    @Test
    void transitiveImportsAreVisible() throws IOException {
        write("c.nh", "function deep() { p.x = 1; }");
        write("b.nh", "import \"c.nh\";\nfunction middle() { deep(); }");
        Path main = write("main.nh", "import \"b.nh\";\nfunction run() { deep(); middle(); }");

        assertThat(functionNames(compile(main))).contains("deep", "middle", "run");
    }

    @Test
    void fileCannotUseItsImportersOtherImports() throws IOException {
        write("c.nh", "function func() { p.x = 1; }");
        write("a.nh", "function useIt() { func(); }");
        Path main = write("main.nh", """
                import "a.nh";
                import "c.nh";
                function run() { useIt(); }
                """);

        CompilingException error = catchThrowableOfType(CompilingException.class, () -> compile(main));

        assertThat(error.getMessage()).contains("a.nh").contains("'func'").contains("c.nh");
        assertThat(error.errorToken.file).isEqualTo("a.nh");
    }

    @Test
    void constantFromUnimportedFileIsRejected() throws IOException {
        write("c.nh", "const long LIMIT = 5;");
        write("a.nh", "function useIt() { p.x = LIMIT; }");
        Path main = write("main.nh", "import \"c.nh\";\nimport \"a.nh\";");

        assertThatThrownBy(() -> compile(main)).isInstanceOf(CompilingException.class).hasMessageContaining("LIMIT");
    }

    @Test
    void errorInImportedFileNamesThatFileAndMapsToRootImport() throws IOException {
        write("lib/broken.nh", "function bad() {\n    p.x = ;\n}");
        Path main = write("main.nh", "\nimport \"lib/broken.nh\";\n");
        NHCompiler compiler = new NHCompiler();

        CompilingException error = catchThrowableOfType(CompilingException.class,
                () -> compiler.analyze(Files.readString(main), main, TestCompiler.DEFAULT_OPTIONS));

        assertThat(error.getMessage()).startsWith("lib/broken.nh, line 2");
        assertThat(compiler.getLastImportGraph().getRootImportToken("lib/broken.nh"))
                .hasValueSatisfying(token -> assertThat(token.line).isEqualTo(2));
    }

    @Test
    void sharedImportIsIncludedOnce() throws IOException {
        write("shared.nh", "function common() { p.x = 1; }");
        write("a.nh", "import \"shared.nh\";\nfunction fromA() { common(); }");
        write("b.nh", "import \"shared.nh\";\nfunction fromB() { common(); }");
        Path main = write("main.nh", "import \"a.nh\";\nimport \"b.nh\";");

        assertThat(functionNames(compile(main))).containsOnlyOnce("common");
    }

    @Test
    void circularImportIsAnError() throws IOException {
        write("a.nh", "import \"b.nh\";");
        write("b.nh", "import \"a.nh\";");
        Path main = write("main.nh", "import \"a.nh\";");

        assertThatThrownBy(() -> compile(main)).hasMessageContaining("Circular import");
    }

    @Test
    void wildcardImportsEveryScriptInFolder() throws IOException {
        write("lib/one.nh", "function one() { p.x = 1; }");
        write("lib/two.nh", "function two() { p.x = 2; }");
        Path main = write("main.nh", "import \"lib/*\";\nfunction run() { one(); two(); }");

        assertThat(functionNames(compile(main))).contains("one", "two", "run");
    }
}
