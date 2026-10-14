package com.drakou111.neohouse.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryManagerTest {
    private static final String TRIG = "neohouse/trig.nh";

    @TempDir
    Path scripts;

    private static String bundledTrig() throws IOException {
        try (InputStream in = LibraryManager.class.getResourceAsStream("/assets/neohouse/library/" + TRIG)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void extractsMissingFiles() throws IOException {
        LibraryManager.init(this.scripts);

        assertThat(Files.readString(this.scripts.resolve(TRIG))).isEqualTo(bundledTrig());
    }

    @Test
    void leavesEditedFilesAlone() throws IOException {
        Path trig = this.scripts.resolve(TRIG);
        Files.createDirectories(trig.getParent());
        Files.writeString(trig, "// my own trig\n");

        LibraryManager.init(this.scripts);

        assertThat(Files.readString(trig)).isEqualTo("// my own trig\n");
    }

    @Test
    void fingerprintIgnoresLineEndings() {
        byte[] unix = "a\nb\n".getBytes(StandardCharsets.UTF_8);
        byte[] windows = "a\r\nb\r\n".getBytes(StandardCharsets.UTF_8);

        assertThat(LibraryManager.fingerprint(windows)).isEqualTo(LibraryManager.fingerprint(unix));
    }

    @Test
    void upgradesAnUnmodifiedOldVersion() throws IOException {
        Path trig = this.scripts.resolve(TRIG);
        Files.createDirectories(trig.getParent());
        try (InputStream old = getClass().getResourceAsStream("/library/trig_1.0.0.nh")) {
            Files.write(trig, old.readAllBytes());
        }

        LibraryManager.init(this.scripts);

        assertThat(Files.readString(trig)).isEqualTo(bundledTrig()).doesNotContain("const function");
    }
}
