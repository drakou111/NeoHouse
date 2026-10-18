package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.CompileOptions;
import com.drakou111.neohouse.config.GlobalCallMode;
import com.drakou111.neohouse.config.MultiCallMode;
import com.drakou111.neohouse.config.OverrideMode;
import com.drakou111.neohouse.config.RecursionMode;
import com.drakou111.neohouse.config.UndeclaredVariableMode;
import com.drakou111.neohouse.config.UnknownCallMode;
import com.drakou111.neohouse.config.UnsetVariableMode;
import com.drakou111.neohouse.ui.HelpContent.Block;
import com.drakou111.neohouse.ui.HelpContent.Code;
import com.drakou111.neohouse.ui.HelpContent.Entry;
import com.drakou111.neohouse.ui.HelpContent.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Every code example the help shows, as a script that compiles on its own. */
public final class HelpExamples {
    /** The settings a new install starts with. */
    public static final CompileOptions DEFAULTS = new CompileOptions(MultiCallMode.ERROR, RecursionMode.WARN,
            GlobalCallMode.ERROR, OverrideMode.IF_EMPTY, UndeclaredVariableMode.ALLOW, false, UnsetVariableMode.WARN,
            UnknownCallMode.WARN);

    private static final Pattern WHOLE_SCRIPT = Pattern.compile(
            "(?m)^(event|function|command|loop|menu|const|enum|long|double|string|bool|var) ");

    /**
     * One example. {@code name} is the topic plus the example's number in it; {@code complete} is
     * false for a snippet that was wrapped in a function to compile.
     */
    public record Example(String name, String script, boolean complete) {}

    private HelpExamples() {
    }

    /** Examples with an import are left out, since the files they import aren't there. */
    public static List<Example> all() {
        List<Example> examples = new ArrayList<>();
        for (Topic topic : HelpContent.topics()) {
            String prefix = topic.title().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            int number = 0;
            for (Block block : topic.blocks()) {
                String code = switch (block) {
                    case Code c when !c.code().contains("import ") -> c.code();
                    case Entry entry when entry.example() != null -> entry.example();
                    default -> null;
                };
                if (code == null) continue;
                number++;
                boolean complete = WHOLE_SCRIPT.matcher(code).find();
                String script = complete ? code : "function helpExample() {\n" + code + "\n}";
                examples.add(new Example("%s-%02d".formatted(prefix, number), script, complete));
            }
        }
        return examples;
    }
}
