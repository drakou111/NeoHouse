package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.ast.main.MainNode;
import com.drakou111.neohouse.compiler.diagnostics.CompileWarning;
import com.drakou111.neohouse.compiler.visitors.HousingNameCollector;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.config.UnknownCallMode;
import com.drakou111.neohouse.config.UnsetVariableMode;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnknownCallTest {

    private static CompileOptions options(UnknownCallMode mode) {
        return new CompileOptions(MultiCallMode.ERROR, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.IF_EMPTY,
                UndeclaredVariableMode.ALLOW, false, UnsetVariableMode.WARN, mode);
    }

    private record Compiled(List<String> warnings, List<String> steps) {}

    private static Compiled compile(String source, UnknownCallMode mode) {
        NHCompiler compiler = new NHCompiler();
        MainNode main = compiler.analyze(source, null, options(mode));
        List<String> warnings = new ArrayList<>();
        for (CompileWarning warning : compiler.getLastWarnings()) warnings.add(warning.message());
        List<String> steps = new ArrayList<>();
        collect(new CodeGenerator(OverrideMode.IF_EMPTY).generate(main), steps);
        return new Compiled(warnings, steps);
    }

    private static void collect(Action action, List<String> names) {
        names.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, names);
    }

    @Test
    void aCallToAFunctionOutsideTheScriptWarnsAndIsCheckedBeforeTheRun() {
        Compiled compiled = compile("event onJoin { greet(); }", UnknownCallMode.WARN);

        assertThat(compiled.warnings()).singleElement().asString().contains("Function 'greet' isn't in this script");
        assertThat(compiled.steps()).contains("Check functions exist: greet");
    }

    @Test
    void offSkipsTheWarningButStillChecks() {
        Compiled compiled = compile("event onJoin { greet(); }", UnknownCallMode.OFF);

        assertThat(compiled.warnings()).isEmpty();
        assertThat(compiled.steps()).contains("Check functions exist: greet");
    }

    @Test
    void errorRejectsIt() {
        assertThatThrownBy(() -> compile("event onJoin { greet(); }", UnknownCallMode.ERROR))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("Function 'greet' isn't in this script");
    }

    @Test
    void argumentsCantBePassedToAFunctionOutsideTheScript() {
        assertThatThrownBy(() -> compile("event onJoin { greet(1); }", UnknownCallMode.OFF))
                .isInstanceOf(CompilingException.class)
                .hasMessageContaining("can't take arguments");
    }

    @Test
    void aValueReturningCallStillNeedsTheFunction() {
        assertThatThrownBy(() -> compile("event onJoin { p.x = score(); }", UnknownCallMode.OFF))
                .isInstanceOf(CompilingException.class);
    }

    @Test
    void unknownCallsWorkInEveryKindOfBody() {
        String source = """
                function a() { if (p.x?0 == 1) { greet(); } else { global reset(); } }
                loop tick(20) { greet(); }
                command nhtestcmd { a(); }
                menu shop(1) { slot0: ({id:"minecraft:stone"}) { reset(); } }
                """;

        // Menu slot items need a running game to build, so this checks what the run would look up.
        NHCompiler compiler = new NHCompiler();
        MainNode main = compiler.analyze(source, null, options(UnknownCallMode.WARN));

        assertThat(compiler.getLastWarnings()).hasSize(4);
        assertThat(new HousingNameCollector().collect(main).get(HousingList.FUNCTIONS)).containsExactly("greet", "reset");
    }

    @Test
    void functionsAndMenusInTheScriptAreNotChecked() {
        String source = """
                function a() { displayMenu("shop"); }
                loop tick(20) { a(); }
                event onJoin { tick(); }
                menu shop(1) { }
                """;

        Compiled compiled = compile(source, UnknownCallMode.WARN);

        assertThat(compiled.warnings()).isEmpty();
        assertThat(compiled.steps()).noneMatch(step -> step.startsWith("Check functions exist")
                || step.startsWith("Check menus exist"));
    }

    @Test
    void aMenuOutsideTheScriptWarnsAndIsChecked() {
        Compiled compiled = compile("event onJoin { displayMenu(\"Item Shop\"); }", UnknownCallMode.WARN);

        assertThat(compiled.warnings()).singleElement().asString().contains("Menu 'Item Shop' isn't in this script");
        assertThat(compiled.steps()).contains("Check menus exist: Item Shop");
    }

    @Test
    void aMenuNameFromAPlaceholderIsLeftAlone() {
        Compiled compiled = compile("event onJoin { displayMenu(\"%player.name%\"); }", UnknownCallMode.WARN);

        assertThat(compiled.warnings()).isEmpty();
        assertThat(compiled.steps()).noneMatch(step -> step.startsWith("Check menus exist"));
    }

    @Test
    void callingTheSameOutsideFunctionTwiceCompiles() {
        Compiled compiled = compile("event onJoin { greet(); greet(); }", UnknownCallMode.OFF);

        assertThat(compiled.steps()).contains("Check functions exist: greet");
    }
}
