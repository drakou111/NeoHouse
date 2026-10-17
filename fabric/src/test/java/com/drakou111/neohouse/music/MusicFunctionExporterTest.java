package com.drakou111.neohouse.music;

import com.drakou111.neohouse.compiler.CodeGenerator;
import com.drakou111.neohouse.compiler.CompileOptions;
import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.ast.main.MainStatementNode;
import com.drakou111.neohouse.compiler.ast.main.StatementNode;
import com.drakou111.neohouse.compiler.ast.main_statements.VoidFunctionNode;
import com.drakou111.neohouse.compiler.ast.statement.ConditionalNode;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.config.UnsetVariableMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusicFunctionExporterTest {

    // Strict settings, so the export must declare what it uses and must not warn.
    private static final CompileOptions STRICT = new CompileOptions(MultiCallMode.ERROR, RecursionMode.WARN,
            GlobalCallMode.ERROR, OverrideMode.IF_EMPTY, UndeclaredVariableMode.ERROR, true, UnsetVariableMode.WARN);
    // Far past one function's 25 conditionals of up to 25 sounds and 30 pauses each.
    private static final int LONG_SONG_NOTES = 1000;

    private static MusicProject song(int notes) {
        MusicProject project = MusicProject.fresh("test");
        for (int i = 0; i < notes; i++) project.tracks.getFirst().notes.add(new MusicNote(i * 2));
        return project;
    }

    private static MusicProject songWithStop(int notes, String stopVariable, boolean inverted) {
        MusicProject project = song(notes);
        project.stopVariable = stopVariable;
        project.stopVariableInverted = inverted;
        return project;
    }

    private static MainNode compileCleanly(String script) {
        NHCompiler compiler = new NHCompiler();
        MainNode main = compiler.analyze(script, null, STRICT);
        assertThat(compiler.getLastWarnings()).isEmpty();
        assertThat(compiler.getLastHelperCount()).isZero();
        new CodeGenerator(OverrideMode.IF_EMPTY).generate(main);
        return main;
    }

    private static List<VoidFunctionNode> functions(MainNode main) {
        List<VoidFunctionNode> functions = new ArrayList<>();
        for (MainStatementNode statement : main.statements)
            if (statement instanceof VoidFunctionNode function) functions.add(function);
        return functions;
    }

    @Test
    void withoutAStopVariableTheActionsAreListedFlat() {
        String script = MusicFunctionExporter.toScript(song(3), "tune");

        assertThat(script).doesNotContain("if (").contains("function tune() {");
        assertThat(script.lines().filter(line -> line.contains("playSound(")).count()).isEqualTo(3);
    }

    @Test
    void everyBlockOfNotesIsGuardedByTheStopVariable() {
        String script = MusicFunctionExporter.toScript(songWithStop(3, "p.stop", false), "tune");

        assertThat(script).contains("long p.stop;", "if (p.stop?0 != 1) {", "} else {\n        return;\n    }");
        MainNode main = compileCleanly(script);
        for (StatementNode statement : functions(main).getFirst().statements.statements)
            assertThat(statement).isInstanceOf(ConditionalNode.class);
    }

    @Test
    void theInvertedCheckPlaysOnlyWhileTheVariableIsOne() {
        String script = MusicFunctionExporter.toScript(songWithStop(3, "g.running", true), "tune");

        assertThat(script).contains("if (g.running?0 == 1) {").doesNotContain("!= 1");
        compileCleanly(script);
    }

    @Test
    void aTeamVariableWorks() {
        compileCleanly(MusicFunctionExporter.toScript(songWithStop(3, "t[red].stop", false), "tune"));
    }

    @Test
    void aLongSongIsSplitIntoChainedPartsTheCompilerLeavesAlone() {
        String script = MusicFunctionExporter.toScript(songWithStop(LONG_SONG_NOTES, "p.stop", false), "tune");

        MainNode main = compileCleanly(script);
        List<VoidFunctionNode> parts = functions(main);
        assertThat(parts.size()).isGreaterThan(1);
        assertThat(parts.getFirst().name).isEqualTo("tune");
        assertThat(script).contains("    tune_part2();\n}", "function tune_part2() {");
        assertThat(script.lines().filter(line -> line.contains("playSound(")).count()).isEqualTo(LONG_SONG_NOTES);
    }

    @Test
    void anEmptySongStillCompiles() {
        compileCleanly(MusicFunctionExporter.toScript(songWithStop(0, "p.stop", false), "tune"));
    }

    @Test
    void anInvalidStopVariableIsRejected() {
        assertThat(MusicFunctionExporter.stopVariableProblem("stop")).isPresent();
        assertThat(MusicFunctionExporter.stopVariableProblem("p.thisNameIsWayTooLong")).isPresent();
        assertThat(MusicFunctionExporter.stopVariableProblem(" p.stop ")).isEmpty();
        assertThat(MusicFunctionExporter.stopVariableProblem("")).isEmpty();
        assertThatThrownBy(() -> MusicFunctionExporter.toScript(songWithStop(1, "x.stop", false), "tune"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
