package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.CheckListLimitAction;
import com.drakou111.neohouse.actions.actiontypes.IfContainerEmptyAction;
import com.drakou111.neohouse.actions.actiontypes.ReadListNamesAction;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorTest {

    private static Action generate(String source, OverrideMode mode) {
        return new CodeGenerator(mode).generate(TestCompiler.analyze(source));
    }

    /** Debug names of every action in the tree, depth first. */
    private static List<String> flatten(Action root) {
        List<String> names = new ArrayList<>();
        collect(root, names);
        return names;
    }

    private static void collect(Action action, List<String> names) {
        names.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, names);
    }

    private static long countOf(Action root, Class<? extends Action> type) {
        List<Action> all = new ArrayList<>();
        collectActions(root, all);
        return all.stream().filter(type::isInstance).count();
    }

    private static void collectActions(Action action, List<Action> all) {
        all.add(action);
        for (Action child : action.getChildren()) collectActions(child, all);
    }

    @Test
    void scriptWithoutFunctionsNeverOpensTheFunctionsList() {
        Action action = generate("event onJoin { p.x = 1; }", OverrideMode.IF_EMPTY);

        assertThat(flatten(action)).noneMatch(name -> name.equals("Send chat: /function"));
        assertThat(countOf(action, ReadListNamesAction.class)).isZero();
    }

    @Test
    void everyTouchedListIsReadOnceAndLimitChecked() {
        String source = """
                function a() { p.x = 1; }
                command coinshop { p.x = 2; }
                menu shop(1) { }
                """;

        Action action = generate(source, OverrideMode.ALWAYS);

        assertThat(flatten(action)).contains("Read functions list", "Read commands list", "Read menus list");
        assertThat(countOf(action, ReadListNamesAction.class)).isEqualTo(3);
        assertThat(countOf(action, CheckListLimitAction.class)).isEqualTo(3);
    }

    @Test
    void ifEmptyChecksExistingFunctionsBeforeFilling() {
        Action action = generate("function a() { p.x = 1; }", OverrideMode.IF_EMPTY);

        assertThat(countOf(action, IfContainerEmptyAction.class)).isEqualTo(1);
    }

    @Test
    void alwaysNeverChecksEmptiness() {
        Action action = generate("function a() { p.x = 1; }", OverrideMode.ALWAYS);

        assertThat(countOf(action, IfContainerEmptyAction.class)).isZero();
    }

    @Test
    void emptyFunctionBodyIsNeverOpened() {
        Action action = generate("function a() { }", OverrideMode.ALWAYS);

        long searchesForA = flatten(action).stream().filter(name -> name.equals("Search: a")).count();
        // Only the delete-if-present search in the functions list; no visit to fill an empty body.
        assertThat(searchesForA).isEqualTo(1);
    }

    @Test
    void unknownSoundNameUsesCustomSound() {
        Action action = generate("function a() { playSound(\"entity.creeper.primed\", 0.7, 1.0, INVOKER); }", OverrideMode.ALWAYS);

        assertThat(flatten(action)).contains("Search: Custom Sound", "Enter text: entity.creeper.primed");
    }

    @Test
    void listedSoundNameUsesTheSoundList() {
        String sound = com.drakou111.neohouse.constants.Sound.values()[0].name();

        Action action = generate("function a() { playSound(\"" + sound + "\", 0.7, 1.0, INVOKER); }", OverrideMode.ALWAYS);

        assertThat(flatten(action)).doesNotContain("Search: Custom Sound");
    }

    @Test
    void variableValuesAreEnteredThroughChatPrompts() {
        Action action = generate("function a() { p.x = 5; }", OverrideMode.ALWAYS);

        assertThat(flatten(action)).contains("Enter text: x", "Enter text: 5");
    }

    @Test
    void tooManyFunctionsIsACompileError() {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i <= 300; i++) source.append("function f").append(i).append("() { p.x = 1; }\n");

        assertThatThrownBy(() -> TestCompiler.analyze(source.toString()))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("more than 300 functions");
    }

    @Test
    void constFunctionKeywordIsGone() {
        assertThatThrownBy(() -> TestCompiler.analyze("const function a() { p.x = 1; }"))
                .isInstanceOf(CompilingException.class);
    }
}
