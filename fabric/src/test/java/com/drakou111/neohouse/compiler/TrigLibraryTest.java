package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatCode;

/** The bundled trig.nh library compiles and its functions can be called, in every multi-call mode that allows it. */
class TrigLibraryTest {
    private static final String TRIG_RESOURCE = "/assets/neohouse/library/neohouse/trig.nh";

    @TempDir
    Path scripts;

    private Path script(String body) throws IOException {
        try (InputStream trig = Objects.requireNonNull(getClass().getResourceAsStream(TRIG_RESOURCE))) {
            Files.copy(trig, this.scripts.resolve("trig.nh"));
        }
        Path main = this.scripts.resolve("main.nh");
        Files.writeString(main, "import \"trig.nh\";\n" + body);
        return main;
    }

    private static void compile(Path file, MultiCallMode mode) throws IOException {
        CompileOptions options = new CompileOptions(mode, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.ALWAYS);
        TestCompiler.analyze(Files.readString(file), file, options);
    }

    @ParameterizedTest
    @EnumSource(value = MultiCallMode.class, names = {"ERROR", "FIX"})
    void facingVectorCanBeCalled(MultiCallMode mode) throws IOException {
        Path main = script("double p.x; double p.y; double p.z;\n"
                + "event onJoin { (p.x, p.y, p.z) = getFacingVector(); }");

        assertThatCode(() -> compile(main, mode)).doesNotThrowAnyException();
    }

    @Test
    void everyFunctionCanBeCalled() throws IOException {
        Path main = script("double p.r;\n"
                + "event onJoin { p.r = sin(1.0); }\n"
                + "event onQuit { p.r = cos(1.0); }\n"
                + "event onDeath { p.r = tan(1.0); }\n"
                + "command trigasin { p.r = asin(0.5); }\n"
                + "command trigacos { p.r = acos(0.5); }\n"
                + "command trigatan { p.r = atan(0.5); }\n"
                + "command trigatan2 { p.r = atan2(1.0, 2.0); }\n");

        assertThatCode(() -> compile(main, MultiCallMode.ERROR)).doesNotThrowAnyException();
    }
}
