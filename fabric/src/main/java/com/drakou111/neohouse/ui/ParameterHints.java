package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The editor's inline parameter-name hints ({@code sin(x: 1)}): which parameter each argument of
 * a call is, for built-in actions and conditions, placeholders taking arguments, and functions
 * declared in the script. User functions are parsed once per edit, since hints are recomputed for
 * every visible line every frame.
 */
final class ParameterHints {

    /** A hint drawn before column {@code charOffset} of a line; the active one is the argument being typed. */
    record InlineHint(int charOffset, String displayText, boolean isActive) {}

    /** The call (by name) and argument index an editing position is in. */
    record CallAt(String functionName, int argIndex) {
        static final CallAt NONE = new CallAt("", -1);
    }

    static final int CONTINUATION_SCAN_LINES = 30;

    private static final Pattern USER_FUNC_PAT =
            Pattern.compile("\\bfunction\\b\\s+(?:\\w+\\s+)?(\\w+)\\s*\\(([^)]*)\\)");
    private static final String FUNCTION_KEYWORD = "function";

    private static final Map<String, List<String>> PLACEHOLDER_PARAMS = Map.ofEntries(
        Map.entry("playerTeamPlayers",   List.of("teamName: string")),
        Map.entry("dateDayTimezone",     List.of("timezone: string")),
        Map.entry("dateMonthTimezone",   List.of("timezone: string")),
        Map.entry("dateYearTimezone",    List.of("timezone: string")),
        Map.entry("dateHourTimezone",    List.of("timezone: string")),
        Map.entry("dateMinuteTimezone",  List.of("timezone: string")),
        Map.entry("dateSecondsTimezone", List.of("timezone: string")),
        Map.entry("randomWholeCapped",   List.of("max: long")),
        Map.entry("randomWholeRanged",   List.of("min: long", "max: long")),
        Map.entry("randomDecimalCapped", List.of("max: double")),
        Map.entry("randomDecimalRanged", List.of("min: double", "max: double"))
    );

    private final Supplier<CharSequence> source;
    private Map<String, List<String>> userFunctions = null;

    ParameterHints(Supplier<CharSequence> source) {
        this.source = source;
    }

    /** Forget the parsed user functions; call after every edit. */
    void invalidate() {
        this.userFunctions = null;
    }

    /**
     * Parameter labels ("name: type") of {@code functionName}: a built-in action or condition, a
     * placeholder taking arguments, or a function declared in the script. Empty when unknown or
     * when it takes no parameters.
     */
    List<String> parameterLabels(String functionName) {
        AddAction action = AddAction.searchByName(functionName);
        if (action != null) return labelsOf(action.params);
        AddCondition condition = AddCondition.searchByName(functionName);
        if (condition != null) return labelsOf(condition.params);
        List<String> placeholder = PLACEHOLDER_PARAMS.get(functionName);
        if (placeholder != null) return placeholder;
        return userFunctions().getOrDefault(functionName, List.of());
    }

    /**
     * The hints for line {@code lineIndex}: arguments of calls on the line itself, and of a call
     * continued from earlier lines. {@code cursorColumn} is the text cursor's column on this line
     * (-1 when elsewhere), so an empty argument slot the cursor is in gets labelled too.
     */
    List<InlineHint> forLine(List<String> lines, int lineIndex, int cursorColumn, CallAt active) {
        String line = lines.get(lineIndex);
        List<InlineHint> hints = new ArrayList<>();
        int definitionParen = functionDefinitionParen(line);

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
            if (c == '"') {
                i++;
                while (i < line.length() && line.charAt(i) != '"') i++;
                if (i < line.length()) i++;
                continue;
            }
            if (!Character.isLetter(c) && c != '_') {
                i++;
                continue;
            }
            int nameStart = i;
            while (i < line.length() && EditorDocument.isIdentifierChar(line.charAt(i))) i++;
            int paren = i;
            while (paren < line.length() && line.charAt(paren) == ' ') paren++;
            if (paren >= line.length() || line.charAt(paren) != '(' || paren == definitionParen) continue;

            String functionName = line.substring(nameStart, i);
            addSlotHints(hints, line, paren + 1, 0, cursorColumn, functionName, active);
            i = paren + 1;
        }

        List<InlineHint> continued = continuationHints(lines, lineIndex, cursorColumn, active);
        if (!continued.isEmpty()) {
            hints.addAll(continued);
            hints.sort(Comparator.comparingInt(InlineHint::charOffset));
        }
        return hints;
    }

    /**
     * The call and argument an argument typed right after {@code before} belongs to, found by
     * scanning back to the unclosed '(' (skipping strings and nested brackets).
     */
    static Optional<CallAt> callAt(String before) {
        int argIndex = 0;
        int depth = 0;
        int braceDepth = 0;
        int paren = -1;
        for (int i = before.length() - 1; i >= 0; i--) {
            char c = before.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && before.charAt(i) != '"') i--;
                continue;
            }
            if (c == '}') braceDepth++;
            else if (c == '{') braceDepth--;
            else if (c == ')') depth++;
            else if (c == '(' && depth > 0) depth--;
            else if (c == '(') {
                paren = i;
                break;
            } else if (c == ',' && depth == 0 && braceDepth == 0) argIndex++;
        }
        if (paren < 0) return Optional.empty();

        String beforeParen = before.substring(0, paren).stripTrailing();
        int nameStart = beforeParen.length();
        while (nameStart > 0 && EditorDocument.isIdentifierChar(beforeParen.charAt(nameStart - 1))) nameStart--;
        String name = beforeParen.substring(nameStart);
        return name.isEmpty() ? Optional.empty() : Optional.of(new CallAt(name, argIndex));
    }

    /**
     * Hints for a line that continues a call's argument list from an earlier line. Only applies when
     * the previous code ends with ',' or '(' inside a still-open call and this line doesn't hold a
     * statement of its own, and it is either indented deeper than the call or followed by the call's
     * ')', so an unfinished call being typed doesn't label the lines below it.
     * Scans a bounded window of earlier lines, skipping strings and comments.
     */
    private List<InlineHint> continuationHints(List<String> lines, int lineIndex, int cursorColumn, CallAt active) {
        if (lineIndex == 0) return List.of();

        // Open brackets as {bracket char, line, column, argument index}, innermost on top.
        Deque<int[]> open = new ArrayDeque<>();
        char lastSignificant = 0;
        for (int li = Math.max(0, lineIndex - CONTINUATION_SCAN_LINES); li < lineIndex; li++) {
            String line = lines.get(li);
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
                if (c == '"') {
                    i++;
                    while (i < line.length() && line.charAt(i) != '"') i++;
                    lastSignificant = '"';
                    continue;
                }
                if (Character.isWhitespace(c)) continue;
                lastSignificant = c;
                switch (c) {
                    case '(', '[', '{' -> open.push(new int[]{c, li, i, 0});
                    case ')', ']', '}' -> { if (!open.isEmpty()) open.pop(); }
                    case ',' -> { if (!open.isEmpty()) open.peek()[3]++; }
                    case ';' -> open.clear(); // a finished statement: nothing before it is still open
                    default -> { }
                }
            }
        }
        if (open.isEmpty() || open.peek()[0] != '(' || (lastSignificant != ',' && lastSignificant != '(')) return List.of();
        int[] call = open.peek();
        if (!CallSpan.continuesCall(lines, call[1], lineIndex, CONTINUATION_SCAN_LINES)) return List.of();

        String callLine = lines.get(call[1]);
        int nameEnd = call[2];
        while (nameEnd > 0 && callLine.charAt(nameEnd - 1) == ' ') nameEnd--;
        int nameStart = nameEnd;
        while (nameStart > 0 && EditorDocument.isIdentifierChar(callLine.charAt(nameStart - 1))) nameStart--;
        if (nameStart >= nameEnd) return List.of();

        List<InlineHint> hints = new ArrayList<>();
        addSlotHints(hints, lines.get(lineIndex), 0, call[3], cursorColumn, callLine.substring(nameStart, nameEnd), active);
        return hints;
    }

    private void addSlotHints(List<InlineHint> hints, String line, int start, int firstArgIndex, int cursorColumn,
                              String functionName, CallAt active) {
        List<String> labels = parameterLabels(functionName);
        if (labels.isEmpty()) return;
        for (CallSpan.ArgSlot slot : CallSpan.argumentSlots(line, start, firstArgIndex, cursorColumn)) {
            if (slot.argIndex() >= labels.size()) continue;
            String label = labels.get(slot.argIndex());
            int colon = label.indexOf(':');
            String name = (colon >= 0 ? label.substring(0, colon) : label).trim();
            boolean isActive = functionName.equals(active.functionName()) && slot.argIndex() == active.argIndex();
            hints.add(new InlineHint(slot.column(), name + ": ", isActive));
        }
    }

    /** Column of the '(' of a function definition on this line ({@code function [type] name(}), or -1. */
    private static int functionDefinitionParen(String line) {
        int keyword = line.indexOf(FUNCTION_KEYWORD);
        if (keyword < 0) return -1;
        int k = keyword + FUNCTION_KEYWORD.length();
        k = skipSpaces(line, k);
        while (k < line.length() && EditorDocument.isIdentifierChar(line.charAt(k))) k++;
        if (k < line.length() && line.charAt(k) == ' ') {
            k = skipSpaces(line, k);
            while (k < line.length() && EditorDocument.isIdentifierChar(line.charAt(k))) k++;
        }
        k = skipSpaces(line, k);
        return k < line.length() && line.charAt(k) == '(' ? k : -1;
    }

    private static int skipSpaces(String line, int from) {
        int k = from;
        while (k < line.length() && line.charAt(k) == ' ') k++;
        return k;
    }

    private Map<String, List<String>> userFunctions() {
        if (this.userFunctions != null) return this.userFunctions;
        Map<String, List<String>> functions = new HashMap<>();
        Matcher m = USER_FUNC_PAT.matcher(this.source.get());
        while (m.find()) {
            // The first declaration of a name wins, as before.
            if (functions.containsKey(m.group(1))) continue;
            List<String> labels = new ArrayList<>();
            String params = m.group(2).trim();
            if (!params.isEmpty()) {
                for (String part : params.split(",")) {
                    String[] tokens = part.trim().split("\\s+");
                    if (tokens.length >= 2) labels.add(tokens[tokens.length - 1] + ": " + tokens[0]);
                    else labels.add(tokens[0]);
                }
            }
            functions.put(m.group(1), labels);
        }
        this.userFunctions = functions;
        return functions;
    }

    private static List<String> labelsOf(List<Param> params) {
        List<String> labels = new ArrayList<>(params.size());
        for (Param p : params) labels.add(p.hintLabel());
        return labels;
    }
}
