package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.constants.Sound;
import com.drakou111.neohouse.music.MusicFunctionExporter;
import com.drakou111.neohouse.music.MusicNote;
import com.drakou111.neohouse.music.MusicProject;
import com.drakou111.neohouse.music.MusicTrack;
import com.drakou111.neohouse.ui.HelpExamples;
import com.drakou111.neohouse.ui.HelpExamples.Example;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;

/**
 * Compiles the help's examples, the shipped library and the scripts in snapshots/cases, and compares
 * everything a run would do (warnings, errors and every generated step) with the saved snapshots, so
 * any change in what reaches Housing shows up. Accept intended changes with
 * {@code ./gradlew :fabric:test -PupdateSnapshots}, then review the snapshot diff before committing.
 */
class CompilerSnapshotTest {
    private static final Path SNAPSHOTS = Path.of("src/test/resources/snapshots");
    private static final Path CASES = SNAPSHOTS.resolve("cases");
    private static final Path EXPECTED = SNAPSHOTS.resolve("expected");
    private static final Path LIBRARY = Path.of("src/main/resources/assets/neohouse/library");
    private static final Pattern MODE_LINE = Pattern.compile("^// mode: (\\w+)");
    private static final String INDENT = "  ";

    private record Script(String source, Path file, OverrideMode mode) {}

    @Test
    void generatedActionsMatchTheSnapshots() throws IOException {
        Map<String, String> actual = new TreeMap<>();
        for (Map.Entry<String, Script> entry : scripts().entrySet())
            actual.put(entry.getKey(), render(entry.getValue()));

        if (Boolean.getBoolean("neohouse.updateSnapshots")) {
            update(actual);
            return;
        }

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : actual.entrySet()) {
            Path file = EXPECTED.resolve(entry.getKey() + ".txt");
            if (!Files.exists(file)) {
                problems.add(entry.getKey() + ": no snapshot yet");
                continue;
            }
            String expected = Files.readString(file, StandardCharsets.UTF_8);
            if (!expected.equals(entry.getValue())) problems.add(entry.getKey() + ": " + firstDifference(expected, entry.getValue()));
        }
        for (String stale : savedNames())
            if (!actual.containsKey(stale)) problems.add(stale + ": snapshot of a script that no longer exists");

        if (!problems.isEmpty())
            fail("%d snapshot(s) differ. If the change is intended, run ./gradlew :fabric:test -PupdateSnapshots and review the diff.%n%s"
                    .formatted(problems.size(), String.join("\n", problems)));
    }

    private static Map<String, Script> scripts() throws IOException {
        Map<String, Script> scripts = new TreeMap<>();
        for (Example example : HelpExamples.all())
            add(scripts, "help/" + example.name(), new Script(example.script(), null, OverrideMode.IF_EMPTY));
        for (Path file : nhFiles(LIBRARY))
            add(scripts, "library/" + nameOf(LIBRARY, file), new Script(read(file), file, OverrideMode.IF_EMPTY));
        for (Path file : nhFiles(CASES)) {
            String source = read(file);
            Matcher mode = MODE_LINE.matcher(source);
            OverrideMode overrideMode = mode.find() ? OverrideMode.valueOf(mode.group(1)) : OverrideMode.IF_EMPTY;
            add(scripts, "cases/" + nameOf(CASES, file), new Script(source, file, overrideMode));
        }
        add(scripts, "music/plain", new Script(MusicFunctionExporter.toScript(song(""), "tune"), null, OverrideMode.IF_EMPTY));
        add(scripts, "music/stop-variable", new Script(MusicFunctionExporter.toScript(song("p.stop"), "tune"), null, OverrideMode.IF_EMPTY));
        return scripts;
    }

    private static void add(Map<String, Script> scripts, String name, Script script) {
        if (scripts.put(name, script) != null) fail("Two scripts would share the snapshot " + name);
    }

    /** A short two-track song with chords, gaps and a long pause, exported with {@code stopVariable}. */
    private static MusicProject song(String stopVariable) {
        MusicProject project = MusicProject.fresh("snapshot");
        project.tracks.add(new MusicTrack("Bass", Sound.BLOCK_NOTE_BLOCK_BASS));
        int[] ticks = {0, 0, 4, 10, 10, 200, 7000};
        for (int i = 0; i < ticks.length; i++) {
            MusicNote note = new MusicNote(ticks[i]);
            note.semitone = i * 3;
            project.tracks.get(i % 2).notes.add(note);
        }
        project.stopVariable = stopVariable;
        return project;
    }

    private static String render(Script script) {
        StringBuilder out = new StringBuilder("== source\n").append(script.source().strip()).append('\n');
        NHCompiler compiler = new NHCompiler();
        try {
            MainNode main = compiler.analyze(script.source(), script.file(), HelpExamples.DEFAULTS);
            Action root = new CodeGenerator(script.mode()).generate(main);
            out.append("== summary\n").append(compiler.getLastSummary().describe()).append('\n');
            appendWarnings(out, compiler.getLastWarnings());
            out.append("== actions\n");
            appendTree(out, root, 0);
        } catch (RuntimeException e) {
            appendWarnings(out, compiler.getLastWarnings());
            out.append("== error\n").append(e.getMessage() == null ? e.toString() : e.getMessage().replace("\r", "")).append('\n');
        }
        return out.toString();
    }

    private static void appendWarnings(StringBuilder out, List<CompileWarning> warnings) {
        if (warnings.isEmpty()) return;
        out.append("== warnings\n");
        for (CompileWarning warning : warnings) {
            String where = warning.token() == null ? "" : "line " + warning.token().line + ": ";
            out.append(where).append(warning.message()).append('\n');
        }
    }

    private static void appendTree(StringBuilder out, Action action, int depth) {
        out.append(INDENT.repeat(depth)).append(action.getDebugName()).append('\n');
        for (Action child : action.getChildren()) appendTree(out, child, depth + 1);
    }

    private static void update(Map<String, String> actual) throws IOException {
        for (String stale : savedNames())
            if (!actual.containsKey(stale)) Files.delete(EXPECTED.resolve(stale + ".txt"));
        for (Map.Entry<String, String> entry : actual.entrySet()) {
            Path file = EXPECTED.resolve(entry.getKey() + ".txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    /** The names of the saved snapshots, as keys like "cases/strings". */
    private static List<String> savedNames() throws IOException {
        if (!Files.isDirectory(EXPECTED)) return List.of();
        try (Stream<Path> files = Files.walk(EXPECTED)) {
            return files.filter(file -> file.toString().endsWith(".txt"))
                    .map(file -> EXPECTED.relativize(file).toString().replace('\\', '/').replaceAll("\\.txt$", ""))
                    .toList();
        }
    }

    private static List<Path> nhFiles(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(file -> file.toString().endsWith(".nh")).sorted().toList();
        }
    }

    private static String nameOf(Path folder, Path file) {
        return folder.relativize(file).toString().replace('\\', '/').replaceAll("\\.nh$", "");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).replace("\r", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String firstDifference(String expected, String actual) {
        String[] want = expected.split("\n", -1);
        String[] got = actual.split("\n", -1);
        for (int i = 0; i < Math.max(want.length, got.length); i++) {
            String a = i < want.length ? want[i] : "<end>";
            String b = i < got.length ? got[i] : "<end>";
            if (!a.equals(b)) return "line %d%n    expected: %s%n    actual:   %s".formatted(i + 1, a, b);
        }
        return "differs";
    }
}
