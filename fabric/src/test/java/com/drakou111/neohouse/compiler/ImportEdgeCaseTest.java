package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportEdgeCaseTest {
    @TempDir
    Path scripts;

    private Path write(String name, String content) throws IOException {
        Path file = this.scripts.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private static void compile(Path file) throws IOException {
        TestCompiler.analyze(Files.readString(file), file, TestCompiler.DEFAULT_OPTIONS);
    }

    @Test
    void importInsideAFunctionIsRejected() throws IOException {
        write("lib.nh", "function helper() { p.x = 1; }");
        Path main = write("main.nh", "function run() {\n    import \"lib.nh\";\n}");

        assertThatThrownBy(() -> compile(main)).isInstanceOf(CompilingException.class).hasMessageContaining("top level");
    }

    @Test
    void missingImportNamesThePath() throws IOException {
        Path main = write("main.nh", "import \"nope.nh\";");

        assertThatThrownBy(() -> compile(main)).hasMessageContaining("Import not found").hasMessageContaining("nope.nh");
    }

    @Test
    void importWithoutExtensionIsRejected() throws IOException {
        Path main = write("main.nh", "import \"lib\";");

        assertThatThrownBy(() -> compile(main)).hasMessageContaining("must end with");
    }

    @Test
    void sameImportTwiceInOneFileWarns() throws IOException {
        write("lib.nh", "function helper() { p.x = 1; }");
        Path main = write("main.nh", "import \"lib.nh\";\nimport \"lib.nh\";\nfunction run() { helper(); }");
        NHCompiler compiler = new NHCompiler();

        compiler.analyze(Files.readString(main), main, TestCompiler.DEFAULT_OPTIONS);

        assertThat(compiler.getLastWarnings()).anyMatch(warning -> warning.message().contains("more than once"));
    }

    @Test
    void importsNeedASavedScript() {
        assertThatThrownBy(() -> TestCompiler.analyze("import \"lib.nh\";"))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("saved");
    }

    @Test
    void globalFromUnimportedFileIsRejected() throws IOException {
        write("c.nh", "long g.score = 0;");
        write("a.nh", "function useIt() { g.score += 1; }");
        Path main = write("main.nh", "import \"c.nh\";\nimport \"a.nh\";");

        assertThatThrownBy(() -> compile(main)).hasMessageContaining("g.score").hasMessageContaining("c.nh");
    }

    @Test
    void localDeclarationShadowsAnInvisibleGlobal() throws IOException {
        write("c.nh", "long p.count;");
        write("a.nh", "import \"c.nh\";\nfunction useIt() { p.count = 1; }");
        Path main = write("main.nh", "import \"a.nh\";\nfunction run() { useIt(); }");

        assertThatCode(() -> compile(main)).doesNotThrowAnyException();
    }

    @Test
    void enumFromUnimportedFileIsRejected() throws IOException {
        write("c.nh", "enum Color { RED, GREEN }");
        write("a.nh", "function useIt() { p.x = Color.RED; }");
        Path main = write("main.nh", "import \"c.nh\";\nimport \"a.nh\";");

        assertThatThrownBy(() -> compile(main)).hasMessageContaining("Color");
    }
}
