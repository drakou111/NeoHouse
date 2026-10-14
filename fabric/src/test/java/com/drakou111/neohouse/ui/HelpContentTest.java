package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.CompileOptions;
import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.ui.HelpContent.Block;
import com.drakou111.neohouse.ui.HelpContent.Code;
import com.drakou111.neohouse.ui.HelpContent.Entry;
import com.drakou111.neohouse.ui.HelpContent.Heading;
import com.drakou111.neohouse.ui.HelpContent.Topic;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HelpContentTest {

    // The settings a new install starts with.
    private static final CompileOptions DEFAULTS = new CompileOptions(
            MultiCallMode.ERROR, RecursionMode.WARN, GlobalCallMode.ERROR, OverrideMode.IF_EMPTY);
    private static final Pattern WHOLE_SCRIPT = Pattern.compile(
            "(?m)^(event|function|command|loop|menu|const|enum|long|double|string|bool|var) ");

    /** Every example the help shows, as a script that compiles on its own. */
    private static List<String> examples() {
        List<String> scripts = new ArrayList<>();
        for (Topic topic : HelpContent.topics()) {
            for (Block block : topic.blocks()) {
                switch (block) {
                    case Code code when !code.code().contains("import ") -> scripts.add(asScript(code.code()));
                    case Entry entry when entry.example() != null -> scripts.add(asScript(entry.example()));
                    default -> { }
                }
            }
        }
        return scripts;
    }

    private static String asScript(String code) {
        return WHOLE_SCRIPT.matcher(code).find() ? code : "function helpExample() {\n" + code + "\n}";
    }

    @Test
    void everyExampleCompiles() {
        List<String> failures = new ArrayList<>();
        for (String script : examples()) {
            try {
                new NHCompiler().analyze(script, null, DEFAULTS);
            } catch (RuntimeException e) {
                failures.add(e.getMessage().lines().findFirst().orElse("") + "\n" + script);
            }
        }
        assertThat(failures).isEmpty();
    }

    @Test
    void referenceCoversActionsConditionsEventsAndPlaceholders() {
        List<String> titles = HelpContent.topics().stream()
                .filter(t -> t.section().equals(HelpContent.REFERENCE)).map(Topic::title).toList();

        assertThat(titles).containsExactly("Actions", "Conditions", "Events", "Placeholders");
    }

    @Test
    void searchKeepsTheHeadingOfAMatch() {
        Topic functions = HelpContent.topics().stream().filter(t -> t.title().equals("Functions")).findFirst().orElseThrow();
        List<Block> found = HelpContent.search(functions, "recursion");

        assertThat(found).first().isEqualTo(new Heading("Recursion"));
        assertThat(found).hasSize(2);
    }

    @Test
    void searchNeedsEveryWord() {
        Topic actions = HelpContent.topics().stream().filter(t -> t.title().equals("Actions")).findFirst().orElseThrow();

        assertThat(HelpContent.search(actions, "playsound volume")).isNotEmpty();
        assertThat(HelpContent.search(actions, "playsound zzzz")).isEmpty();
    }
}
