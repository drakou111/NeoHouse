package com.drakou111.neohouse.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scripts and music used to live in separate folders (neohouse-scripts, neohouse-music). This moves
 * the old music folder's contents into the scripts folder, keeping subfolders. A file is never
 * overwritten: a name already taken becomes "name (2).ext". The old folder is removed once empty.
 */
public final class MusicFolderMerge {
    private MusicFolderMerge() {}

    /** @return how many files were moved */
    public static int merge(Path oldMusicDir, Path scriptsDir) throws IOException {
        if (!Files.isDirectory(oldMusicDir)) return 0;

        List<Path> files;
        try (Stream<Path> walk = Files.walk(oldMusicDir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            Path target = freeName(scriptsDir.resolve(oldMusicDir.relativize(file).toString()));
            Files.createDirectories(target.getParent());
            Files.move(file, target);
        }

        List<Path> dirs;
        try (Stream<Path> walk = Files.walk(oldMusicDir)) {
            dirs = walk.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList();
        }
        for (Path dir : dirs) {
            try (Stream<Path> left = Files.list(dir)) {
                if (left.findAny().isEmpty()) Files.delete(dir);
            }
        }
        return files.size();
    }

    /** {@code target}, or the first "name (N).ext" next to it that doesn't exist yet. */
    static Path freeName(Path target) {
        if (!Files.exists(target)) return target;
        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int n = 2; ; n++) {
            Path candidate = target.resolveSibling(base + " (" + n + ")" + ext);
            if (!Files.exists(candidate)) return candidate;
        }
    }
}
