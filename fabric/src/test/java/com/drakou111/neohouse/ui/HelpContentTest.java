package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.NHCompiler;
import com.drakou111.neohouse.ui.HelpContent.Block;
import com.drakou111.neohouse.ui.HelpContent.Heading;
import com.drakou111.neohouse.ui.HelpContent.Preformatted;
import com.drakou111.neohouse.ui.HelpContent.Topic;
import com.drakou111.neohouse.ui.HelpExamples.Example;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelpContentTest {

    @Test
    void everyExampleCompiles() {
        List<String> failures = new ArrayList<>();
        for (Example example : HelpExamples.all()) {
            try {
                new NHCompiler().analyze(example.script(), null, HelpExamples.DEFAULTS);
            } catch (RuntimeException e) {
                failures.add(e.getMessage().lines().findFirst().orElse("") + "\n" + example.script());
            }
        }
        assertThat(failures).isEmpty();
    }

    /** Complete scripts (not snippets) are what people copy into a new file, so they shouldn't warn. */
    @Test
    void everyCompleteExampleCompilesWithoutWarnings() {
        List<String> warned = new ArrayList<>();
        for (Example example : HelpExamples.all()) {
            if (!example.complete()) continue;
            NHCompiler compiler = new NHCompiler();
            compiler.analyze(example.script(), null, HelpExamples.DEFAULTS);
            if (!compiler.getLastWarnings().isEmpty())
                warned.add(compiler.getLastWarnings().getFirst().message() + "\n" + example.script());
        }
        assertThat(warned).isEmpty();
    }

    /** A stray double space inside a cell would split it into a column of its own. */
    @Test
    void everyTableHasAtMostThreeColumns() {
        for (Topic topic : HelpContent.topics()) {
            for (Block block : topic.blocks()) {
                if (!(block instanceof Preformatted table)) continue;
                int columns = 0;
                for (List<TextTable.Cell> line : TextTable.parse(table.text()))
                    for (TextTable.Cell cell : line) columns = Math.max(columns, cell.column() + 1);
                assertThat(columns).as(topic.title() + ":\n" + table.text()).isLessThanOrEqualTo(3);
            }
        }
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
