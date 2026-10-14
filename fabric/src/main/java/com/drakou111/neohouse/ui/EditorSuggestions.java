package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.constants.Teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the editor suggests for the text before the cursor: event names after {@code event},
 * placeholder names after {@code ph.}, location presets for a location argument, and the allowed
 * values of an enum-like string argument.
 */
final class EditorSuggestions {

    /**
     * Suggested completions; applying one replaces the last {@code typedLength} characters. Inside a
     * string the closing quote is added if missing.
     */
    record Suggestions(List<String> items, int typedLength, boolean insideString) {}

    private static final Pattern PLACEHOLDER_PAT = Pattern.compile("ph\\.(\\w*)$");
    private static final Pattern TRAILING_WORD_PAT = Pattern.compile("([A-Za-z_]*)$");
    // "event " at the start of a statement, then the (possibly empty) event name being typed.
    private static final Pattern EVENT_NAME_PAT = Pattern.compile("(?:^|[\\s;{}])event\\s+(\\w*)$");

    private EditorSuggestions() {
    }

    /** Suggestions for the line text before the cursor; empty when there is nothing useful to offer. */
    static Optional<Suggestions> at(String beforeCursor) {
        Optional<Suggestions> found = compute(beforeCursor);
        if (found.isEmpty()) return found;
        Suggestions s = found.get();
        // Nothing to offer when the only suggestion is exactly what's already typed.
        boolean alreadyTyped = s.items().size() == 1 && !s.insideString()
                && beforeCursor.endsWith(s.items().getFirst()) && s.typedLength() == s.items().getFirst().length();
        return alreadyTyped ? Optional.empty() : found;
    }

    private static Optional<Suggestions> compute(String beforeCursor) {
        Matcher event = EVENT_NAME_PAT.matcher(beforeCursor);
        if (event.find()) {
            String partial = event.group(1);
            List<String> events = new ArrayList<>();
            for (Event e : Event.values()) if (e.name.startsWith(partial)) events.add(e.name);
            return events.isEmpty() ? Optional.empty() : Optional.of(new Suggestions(events, partial.length(), false));
        }

        Matcher placeholder = PLACEHOLDER_PAT.matcher(beforeCursor);
        if (placeholder.find()) {
            String partial = placeholder.group(1);
            List<String> names = new ArrayList<>();
            for (Placeholder p : Placeholder.values()) if (p.name.startsWith(partial)) names.add(p.name);
            if (!names.isEmpty()) return Optional.of(new Suggestions(names, partial.length(), false));
        }

        int quotes = 0;
        for (int i = 0; i < beforeCursor.length(); i++) if (beforeCursor.charAt(i) == '"') quotes++;
        return quotes % 2 == 0 ? locationPresets(beforeCursor) : stringValues(beforeCursor);
    }

    /** A bare location preset (teleport(INVOKER, ...)): the preset names. */
    private static Optional<Suggestions> locationPresets(String beforeCursor) {
        Matcher word = TRAILING_WORD_PAT.matcher(beforeCursor);
        if (!word.find()) return Optional.empty();
        String partial = word.group(1);
        String beforeWord = beforeCursor.substring(0, word.start(1)).stripTrailing();
        if (!beforeWord.endsWith("(") && !beforeWord.endsWith(",")) return Optional.empty();
        Optional<Param> param = parameterAt(beforeCursor.substring(0, word.start(1)));
        if (param.isEmpty() || param.get().kind() != ActionParamKind.LOCATION) return Optional.empty();

        List<String> presets = new ArrayList<>();
        for (Teleport t : Teleport.values())
            if (t != Teleport.CUSTOM && t.name().startsWith(partial)) presets.add(t.name());
        return presets.isEmpty() ? Optional.empty() : Optional.of(new Suggestions(presets, partial.length(), false));
    }

    /** Inside a string argument: the values the parameter allows. */
    private static Optional<Suggestions> stringValues(String beforeCursor) {
        int quote = beforeCursor.lastIndexOf('"');
        String typed = beforeCursor.substring(quote + 1);
        Optional<Param> param = parameterAt(beforeCursor.substring(0, quote));
        String[] allowed = param.map(Param::enumValues).orElse(null);
        if (allowed == null) return Optional.empty();

        List<String> values = new ArrayList<>();
        for (String value : allowed) if (value.startsWith(typed)) values.add(value);
        return values.isEmpty() ? Optional.empty() : Optional.of(new Suggestions(values, typed.length(), true));
    }

    /** The built-in action or condition parameter an argument starting after {@code before} fills. */
    private static Optional<Param> parameterAt(String before) {
        Optional<ParameterHints.CallAt> call = ParameterHints.callAt(before);
        if (call.isEmpty()) return Optional.empty();
        String name = call.get().functionName();
        int index = call.get().argIndex();
        AddAction action = AddAction.searchByName(name);
        if (action != null) return index < action.params.size() ? Optional.of(action.params.get(index)) : Optional.empty();
        AddCondition condition = AddCondition.searchByName(name);
        if (condition != null) return index < condition.params.size() ? Optional.of(condition.params.get(index)) : Optional.empty();
        return Optional.empty();
    }
}
