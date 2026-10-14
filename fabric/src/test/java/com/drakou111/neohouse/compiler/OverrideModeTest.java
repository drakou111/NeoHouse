package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.IfContainerEmptyAction;
import com.drakou111.neohouse.actions.actiontypes.IfExistsAction;
import com.drakou111.neohouse.actions.actiontypes.RunIfCreatedAction;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OverrideModeTest {

    private static List<Action> all(String source, OverrideMode mode) {
        List<Action> actions = new ArrayList<>();
        collect(new CodeGenerator(mode).generate(TestCompiler.analyze(source)), actions);
        return actions;
    }

    private static void collect(Action action, List<Action> into) {
        into.add(action);
        for (Action child : action.getChildren()) collect(child, into);
    }

    private static List<String> names(List<Action> actions) {
        List<String> names = new ArrayList<>();
        for (Action action : actions) names.add(action.getDebugName());
        return names;
    }

    private static long count(List<Action> actions, Class<?> type) {
        return actions.stream().filter(type::isInstance).count();
    }

    @Test
    void neverOnlyFillsWhatItCreated() {
        List<Action> actions = all("function a() { p.x = 1; }", OverrideMode.NEVER);

        assertThat(count(actions, RunIfCreatedAction.class)).isEqualTo(1);
        assertThat(count(actions, IfContainerEmptyAction.class)).isZero();
        assertThat(names(actions)).anyMatch(name -> name.contains("Skipped function 'a'"));
    }

    @Test
    void ifEmptyChecksExistingCommands() {
        // (Menus with slots can't be generated in tests: placing an item needs a live player.)
        List<Action> actions = all("command coinshop { p.x = 1; }", OverrideMode.IF_EMPTY);

        assertThat(count(actions, IfContainerEmptyAction.class)).isEqualTo(1);
    }

    @Test
    void ifEmptyChecksEventsBeforeFilling() {
        assertThat(count(all("event onJoin { p.x = 1; }", OverrideMode.IF_EMPTY), IfContainerEmptyAction.class)).isEqualTo(1);
    }

    @Test
    void alwaysClearsEvents() {
        List<Action> actions = all("event onJoin { p.x = 1; }", OverrideMode.ALWAYS);

        assertThat(count(actions, IfContainerEmptyAction.class)).isZero();
        assertThat(names(actions)).anyMatch(name -> name.contains("DeleteAll"));
    }

    @Test
    void everyEntryIsCheckedAgainstTheList() {
        String source = """
                function a() { p.x = 1; }
                loop tick(20) { p.x = 2; }
                command coinshop { p.x = 3; }
                """;

        assertThat(count(all(source, OverrideMode.ALWAYS), IfExistsAction.class)).isEqualTo(3);
    }

    @Test
    void moreThanFiftyCommandsIsACompileError() {
        List<String> commands = new ArrayList<>();
        for (int i = 0; i <= 50; i++) commands.add("command cmd" + i + "x { p.x = 1; }");

        assertThatThrownBy(() -> TestCompiler.analyze(String.join("\n", commands)))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("50 commands");
    }

    @Test
    void emptyEventIsLeftAloneWhenPreserving() {
        assertThat(all("event onJoin { }", OverrideMode.IF_EMPTY)).hasSize(1);
        assertThat(Collections.frequency(names(all("event onJoin { }", OverrideMode.NEVER)), "Send chat: /eventaction")).isZero();
    }
}
