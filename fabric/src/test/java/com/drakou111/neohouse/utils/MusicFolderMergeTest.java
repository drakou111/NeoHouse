package com.drakou111.neohouse.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MusicFolderMergeTest {
    @TempDir
    Path game;

    @Test
    void movesMusicIntoTheScriptsFolderWithoutOverwriting() throws IOException {
        Path music = Files.createDirectories(this.game.resolve("neohouse-music/songs"));
        Path scripts = Files.createDirectories(this.game.resolve("neohouse-scripts"));
        Files.writeString(music.resolve("a.nhm"), "song a");
        Files.writeString(music.getParent().resolve("theme.nhm"), "music theme");
        Files.writeString(scripts.resolve("theme.nhm"), "already here");

        int moved = MusicFolderMerge.merge(this.game.resolve("neohouse-music"), scripts);

        assertThat(moved).isEqualTo(2);
        assertThat(Files.readString(scripts.resolve("songs/a.nhm"))).isEqualTo("song a");
        assertThat(Files.readString(scripts.resolve("theme.nhm"))).isEqualTo("already here");
        assertThat(Files.readString(scripts.resolve("theme (2).nhm"))).isEqualTo("music theme");
        assertThat(this.game.resolve("neohouse-music")).doesNotExist();
    }

    @Test
    void nothingToDoWithoutAnOldFolder() throws IOException {
        assertThat(MusicFolderMerge.merge(this.game.resolve("missing"), this.game)).isZero();
    }
}
