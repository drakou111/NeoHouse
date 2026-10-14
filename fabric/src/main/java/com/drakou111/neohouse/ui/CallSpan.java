package com.drakou111.neohouse.ui;

import java.util.List;

/** Text scans the editor uses to tell a call split over several lines from one still being typed. */
public final class CallSpan {

    private CallSpan() {
    }

    /**
     * Whether a call left open on an earlier line is closed by a {@code )} starting from line
     * {@code fromLine}, before a {@code ;}, a block brace or a stray closing bracket ends the
     * statement. Strings and {@code //} comments are skipped; only {@code maxLines} lines are read.
     */
    public static boolean closesBeforeStatementEnds(List<String> lines, int fromLine, int maxLines) {
        int depth = 0;
        int end = Math.min(lines.size(), fromLine + maxLines);
        for (int li = fromLine; li < end; li++) {
            String line = lines.get(li);
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
                if (c == '"') {
                    i++;
                    while (i < line.length() && line.charAt(i) != '"') i++;
                    continue;
                }
                switch (c) {
                    case '(', '[' -> depth++;
                    case '{' -> {
                        if (depth == 0) return false;
                        depth++;
                    }
                    case ')' -> {
                        if (depth == 0) return true;
                        depth--;
                    }
                    case ']', '}' -> {
                        if (depth == 0) return false;
                        depth--;
                    }
                    case ';' -> {
                        if (depth == 0) return false;
                    }
                    default -> { }
                }
            }
        }
        return false;
    }
}
