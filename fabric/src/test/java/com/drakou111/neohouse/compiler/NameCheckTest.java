package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compiler.visitors.HousingNameCollector;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.constants.HousingList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NameCheckTest {
    private static final String SOURCE = """
            long t["Red"].score = 0;
            function f() {
                setTeam("Blue");
                applyLayout("Kit");
                setGroup("VIP", false);
                if (hasTeam(false, "Green") && isGroup(false, "Admin", false)) { t["Red"].score += 1; }
            }
            """;

    private static List<String> names(Action action) {
        List<String> names = new ArrayList<>();
        collect(action, names);
        return names;
    }

    private static void collect(Action action, List<String> into) {
        into.add(action.getDebugName());
        for (Action child : action.getChildren()) collect(child, into);
    }

    @Test
    void collectsTeamsLayoutsAndGroups() {
        Map<HousingList, Set<String>> found = new HousingNameCollector().collect(TestCompiler.analyze(SOURCE));

        assertThat(found.get(HousingList.TEAMS)).containsExactlyInAnyOrder("Red", "Blue", "Green");
        assertThat(found.get(HousingList.LAYOUTS)).containsExactly("Kit");
        assertThat(found.get(HousingList.GROUPS)).containsExactlyInAnyOrder("VIP", "Admin");
    }

    @Test
    void checksComeBeforeAnythingElse() {
        List<String> names = names(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze(SOURCE)));

        int firstCheck = names.indexOf(names.stream().filter(n -> n.startsWith("Check ")).findFirst().orElseThrow());
        int firstFunctionsList = names.indexOf("Send chat: /function");
        assertThat(firstCheck).isLessThan(firstFunctionsList);
        assertThat(names).contains("Send chat: /teams", "Send chat: /layout", "Send chat: /menu", "Search: Permissions and Groups");
    }

    @Test
    void scriptsWithoutNamesSkipTheChecks() {
        List<String> names = names(new CodeGenerator(OverrideMode.ALWAYS).generate(TestCompiler.analyze("function f() { p.x = 1; }")));

        assertThat(names).noneMatch(n -> n.startsWith("Check "));
    }
}
