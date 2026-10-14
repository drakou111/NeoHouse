package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.List;

/** Text scans the editor uses to tell a call split over several lines from one still being typed. */
public final class CallSpan {

    /** An argument position to label: the column its hint goes at and which parameter it is. */
    public record ArgSlot(int column, int argIndex) {}

    private CallSpan() {
    }

    /**
     * The argument slots of a call on {@code line}, starting at column {@code start} (just past the
     * '(' or the start of a continuation line) with argument {@code firstArgIndex}. A slot with text
     * is labelled where its text starts; an empty one only while the text cursor
     * ({@code cursorColumn}, -1 when it isn't on this line) is in it, so the next parameter's name
     * shows as soon as ',' is typed. Stops at the call's ')'; a ';' outside brackets means the line
     * isn't part of the call, so nothing is returned.
     */
    public static List<ArgSlot> argumentSlots(String line, int start, int firstArgIndex, int cursorColumn) {
        List<ArgSlot> slots = new ArrayList<>();
        int argIndex = firstArgIndex;
        int slotStart = start;
        int i = start;
        int depth = 0;
        boolean slotOpen = true;
        while (true) {
            if (slotOpen) {
                while (i < line.length() && line.charAt(i) == ' ') i++;
                boolean empty = i >= line.length() || line.charAt(i) == ')' || line.charAt(i) == ','
                        || (line.charAt(i) == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/');
                if (!empty || (cursorColumn >= slotStart && cursorColumn <= i)) slots.add(new ArgSlot(i, argIndex));
                slotOpen = false;
            }
            if (i >= line.length()) return slots;
            char c = line.charAt(i);
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') return slots;
            if (c == '"') {
                i++;
                while (i < line.length() && line.charAt(i) != '"') i++;
                if (i < line.length()) i++;
                continue;
            }
            switch (c) {
                case '(', '[', '{' -> depth++;
                case ')', ']', '}' -> {
                    if (depth == 0) return slots;
                    depth--;
                }
                case ';' -> {
                    if (depth == 0) return List.of();
                }
                case ',' -> {
                    if (depth == 0) {
                        argIndex++;
                        slotStart = i + 1;
                        slotOpen = true;
                    }
                }
                default -> { }
            }
            i++;
        }
    }

    /**
     * Whether line {@code lineIndex} is part of the arguments of a call opened on line
     * {@code callLine}: either it is indented deeper than the call (the call's {@code )} isn't
     * typed yet while its arguments are written), or the call's {@code )} already follows. An
     * unfinished call followed by a statement at the same indentation isn't continued.
     */
    public static boolean continuesCall(List<String> lines, int callLine, int lineIndex, int maxLines) {
        return indentOf(lines.get(lineIndex)) > indentOf(lines.get(callLine))
                || closesBeforeStatementEnds(lines, lineIndex, maxLines);
    }

    private static int indentOf(String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) indent++;
        return indent;
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
