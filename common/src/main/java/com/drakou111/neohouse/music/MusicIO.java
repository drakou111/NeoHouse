package com.drakou111.neohouse.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MusicIO {

    public static final String EXTENSION = ".nhm";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MusicIO() {}

    public static MusicProject load(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        MusicProject project = GSON.fromJson(json, MusicProject.class);
        if (project == null) project = new MusicProject();
        sanitize(project);
        if (project.name == null || project.name.isBlank()) {
            project.name = stripExtension(file.getFileName().toString());
        }
        return project;
    }

    public static void save(Path file, MusicProject project) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(project), StandardCharsets.UTF_8);
    }

    private static void sanitize(MusicProject p) {
        if (p.tracks == null) p.tracks = new java.util.ArrayList<>();
        if (p.gridDivision <= 0) p.gridDivision = 4;
        p.tracks.removeIf(Objects::isNull);
        for (MusicTrack t : p.tracks) {
            if (t.notes == null) t.notes = new java.util.ArrayList<>();
            if (t.sound == null) t.sound = com.drakou111.neohouse.constants.Sound.BLOCK_NOTE_BLOCK_HARP.name();
            if (t.name == null) t.name = "Track";
            t.notes.removeIf(Objects::isNull);
        }
    }

    public static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
