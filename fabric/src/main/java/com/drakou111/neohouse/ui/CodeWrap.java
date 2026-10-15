package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Wraps a line of code that is too wide to show, breaking after the ", " between arguments so each
 * piece stays readable. Continuation lines are indented one level deeper than the line itself. A
 * piece with no comma to break at is left whole, even if it is still too wide.
 */
final class CodeWrap {
    private static final String SEPARATOR = ", ";
    private static final String CONTINUATION_INDENT = "    ";

    private CodeWrap() {
    }

    static List<String> wrap(String line, int maxWidth, ToIntFunction<String> width) {
        if (width.applyAsInt(line) <= maxWidth) return List.of(line);

        String indent = line.substring(0, line.length() - line.stripLeading().length()) + CONTINUATION_INDENT;
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int from = 0;
        while (from < line.length()) {
            int comma = nextSeparator(line, from);
            // Each piece keeps its comma; the space after it becomes the line break.
            String piece = comma < 0 ? line.substring(from) : line.substring(from, comma + 1);
            from = comma < 0 ? line.length() : comma + SEPARATOR.length();

            String joined = current.isEmpty() ? piece : current + " " + piece;
            if (!current.isEmpty() && width.applyAsInt(joined) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(indent).append(piece);
            } else {
                current = new StringBuilder(joined);
            }
        }
        lines.add(current.toString());
        return lines;
    }

    /** The next ", " at or after {@code from} that isn't inside a string literal, or -1. */
    private static int nextSeparator(String line, int from) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && inString) {
                i++;
                continue;
            }
            if (c == '"') inString = !inString;
            else if (!inString && i >= from && line.startsWith(SEPARATOR, i)) return i;
        }
        return -1;
    }
}
