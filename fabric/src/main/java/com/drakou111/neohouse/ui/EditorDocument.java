package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The code editor's text, cursor and selection, with its edit operations and undo history. Lines
 * and their start offsets are cached until the next edit. Every edit records an undo step and then
 * runs the change listener, so the widget can refresh highlighting, matches and suggestions.
 */
final class EditorDocument {
    private final StringBuilder text = new StringBuilder();
    private final EditorUndoManager undoManager = new EditorUndoManager();
    private final Runnable onTextChanged;
    private int cursor = 0;
    private int selAnchor = 0;
    private List<String> lineCache = null;
    private int[] offsetCache = null;

    EditorDocument(Runnable onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    // ---- reading ----

    String getText() {
        return this.text.toString();
    }

    /** The live text, for scans that shouldn't copy it; don't keep it across edits. */
    CharSequence chars() {
        return this.text;
    }

    int length() {
        return this.text.length();
    }

    int getCursor() {
        return this.cursor;
    }

    int getSelAnchor() {
        return this.selAnchor;
    }

    boolean hasSelection() {
        return this.cursor != this.selAnchor;
    }

    int selectionStart() {
        return Math.min(this.cursor, this.selAnchor);
    }

    int selectionEnd() {
        return Math.max(this.cursor, this.selAnchor);
    }

    String selectedText() {
        return this.text.substring(selectionStart(), selectionEnd());
    }

    /** The text split at '\n' (at least one line); the same list until the next edit. */
    List<String> getLines() {
        if (this.lineCache != null) return this.lineCache;
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= this.text.length(); i++) {
            if (i == this.text.length() || this.text.charAt(i) == '\n') {
                lines.add(this.text.substring(start, i));
                start = i + 1;
            }
        }
        this.lineCache = lines;
        return lines;
    }

    /** Offset of the first character of each line. */
    int[] lineOffsets() {
        if (this.offsetCache != null) return this.offsetCache;
        List<String> lines = getLines();
        int[] offsets = new int[lines.size()];
        int offset = 0;
        for (int i = 0; i < lines.size(); i++) {
            offsets[i] = offset;
            offset += lines.get(i).length() + 1;
        }
        this.offsetCache = offsets;
        return offsets;
    }

    /** {line, column} of {@code offset}; the column is clamped to the line's length. */
    int[] lineColOf(int offset) {
        int[] offsets = lineOffsets();
        int clamped = Math.max(0, offset);
        int found = Arrays.binarySearch(offsets, clamped);
        int line = found >= 0 ? found : Math.max(0, -found - 2);
        return new int[]{line, Math.min(clamped - offsets[line], getLines().get(line).length())};
    }

    /** {line, column} of the cursor. */
    int[] cursorLineCol() {
        return lineColOf(this.cursor);
    }

    // ---- cursor and selection ----

    /** Moves the cursor (clamped to the text); without {@code select} the selection collapses onto it. */
    void moveCursor(int pos, boolean select) {
        this.cursor = clamp(pos);
        if (!select) this.selAnchor = this.cursor;
    }

    /** Selects from {@code anchor} to {@code cursor} (the cursor end is where typing continues). */
    void select(int anchor, int cursor) {
        this.selAnchor = clamp(anchor);
        this.cursor = clamp(cursor);
    }

    void selectAll() {
        select(0, this.text.length());
    }

    /** Collapses the selection onto its start or end, as Left/Right do when text is selected. */
    void collapseSelection(boolean toStart) {
        this.cursor = toStart ? selectionStart() : selectionEnd();
        this.selAnchor = this.cursor;
    }

    /** Selects the identifier around {@code pos}; elsewhere just places the cursor there. */
    void selectWordAt(int pos) {
        int len = this.text.length();
        if (len == 0) return;
        int p = Math.max(0, Math.min(pos, len - 1));
        if (!isIdentifierChar(this.text.charAt(p))) {
            moveCursor(pos, false);
            return;
        }
        int lo = p;
        int hi = p;
        while (lo > 0 && isIdentifierChar(this.text.charAt(lo - 1))) lo--;
        while (hi < len && isIdentifierChar(this.text.charAt(hi))) hi++;
        select(lo, hi);
    }

    /** Where Ctrl+Left goes from {@code from}: the start of the previous word or symbol run. */
    int wordLeftOf(int from) {
        int pos = from;
        if (pos <= 0) return 0;
        pos--;
        while (pos > 0 && Character.isWhitespace(this.text.charAt(pos))) pos--;
        if (isIdentifierChar(this.text.charAt(pos))) {
            while (pos > 0 && isIdentifierChar(this.text.charAt(pos - 1))) pos--;
        } else {
            while (pos > 0 && !Character.isLetterOrDigit(this.text.charAt(pos - 1)) && !Character.isWhitespace(this.text.charAt(pos - 1)))
                pos--;
        }
        return pos;
    }

    /** Where Ctrl+Right goes from {@code from}: the end of the next word or symbol run. */
    int wordRightOf(int from) {
        int pos = from;
        int len = this.text.length();
        if (pos >= len) return pos;
        while (pos < len && Character.isWhitespace(this.text.charAt(pos))) pos++;
        if (pos < len && isIdentifierChar(this.text.charAt(pos))) {
            while (pos < len && isIdentifierChar(this.text.charAt(pos))) pos++;
        } else {
            while (pos < len && !Character.isLetterOrDigit(this.text.charAt(pos)) && !Character.isWhitespace(this.text.charAt(pos)))
                pos++;
        }
        return pos;
    }

    // ---- edits ----

    /** Replaces the whole text without an undo step (loading a file); the cursor goes to the start. */
    void setText(String newText) {
        this.text.setLength(0);
        this.text.append(newText);
        this.cursor = 0;
        this.selAnchor = 0;
        invalidate();
    }

    /** Types {@code s} over the selection. */
    void insert(String s) {
        if (s.isEmpty()) return;
        this.undoManager.push(this.text.toString(), this.cursor);
        deleteSelectionNoUndo();
        this.text.insert(this.cursor, s);
        this.cursor += s.length();
        this.selAnchor = this.cursor;
        changed();
    }

    /** Enter: a line break, followed by the current line's indentation when {@code autoIndent}. */
    void insertNewline(boolean autoIndent) {
        String indent = "";
        if (autoIndent) {
            String line = getLines().get(cursorLineCol()[0]);
            indent = line.substring(0, leadingSpaces(line, line.length()));
        }
        insert("\n" + indent);
    }

    void deleteBackward() {
        if (deleteSelection()) return;
        if (this.cursor <= 0) return;
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.deleteCharAt(this.cursor - 1);
        this.cursor--;
        this.selAnchor = this.cursor;
        changed();
    }

    void deleteForward() {
        if (deleteSelection()) return;
        if (this.cursor >= this.text.length()) return;
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.deleteCharAt(this.cursor);
        changed();
    }

    void deleteWordBackward() {
        if (deleteSelection()) return;
        int start = wordLeftOf(this.cursor);
        if (start == this.cursor) return;
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.delete(start, this.cursor);
        this.cursor = start;
        this.selAnchor = start;
        changed();
    }

    /**
     * Removes the selection, or the cursor's whole line (with its line break) when nothing is
     * selected, and returns what was removed so it can go to the clipboard.
     */
    String cutSelectionOrLine() {
        if (!hasSelection()) {
            int line = cursorLineCol()[0];
            int start = lineOffsets()[line];
            int end = start + getLines().get(line).length();
            if (end < this.text.length()) end++;
            select(start, end);
        }
        String removed = selectedText();
        this.undoManager.push(this.text.toString(), this.cursor);
        deleteSelectionNoUndo();
        changed();
        return removed;
    }

    /** Replaces {@code [from, to)} with {@code replacement}; the cursor ends up after it. */
    void replace(int from, int to, String replacement) {
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.replace(from, to, replacement);
        this.cursor = from + replacement.length();
        this.selAnchor = this.cursor;
        changed();
    }

    /** Replaces the whole text as one undo step, keeping the cursor where it was (clamped). */
    void replaceAll(String newText) {
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.setLength(0);
        this.text.append(newText);
        this.cursor = clamp(this.cursor);
        this.selAnchor = this.cursor;
        changed();
    }

    /** Tab with a selection: indents every selected line by {@code width} spaces. */
    void indentSelectedLines(int width) {
        int[] range = selectedLineRange();
        int[] offsets = lineOffsets();
        String indent = " ".repeat(width);
        this.undoManager.push(this.text.toString(), this.cursor);
        for (int line = range[1]; line >= range[0]; line--) this.text.insert(offsets[line], indent);
        this.cursor = shiftForIndent(this.cursor, offsets, range, width);
        this.selAnchor = shiftForIndent(this.selAnchor, offsets, range, width);
        changed();
    }

    /** Shift+Tab with a selection: removes up to {@code width} leading spaces from every selected line. */
    void dedentSelectedLines(int width) {
        int[] range = selectedLineRange();
        int[] offsets = lineOffsets();
        List<String> lines = getLines();
        int[] removed = new int[range[1] - range[0] + 1];
        boolean any = false;
        for (int line = range[0]; line <= range[1]; line++) {
            removed[line - range[0]] = leadingSpaces(lines.get(line), width);
            any |= removed[line - range[0]] > 0;
        }
        if (!any) return;

        this.undoManager.push(this.text.toString(), this.cursor);
        for (int line = range[1]; line >= range[0]; line--) {
            int r = removed[line - range[0]];
            if (r > 0) this.text.delete(offsets[line], offsets[line] + r);
        }
        this.cursor = shiftForDedent(this.cursor, offsets, range, removed);
        this.selAnchor = shiftForDedent(this.selAnchor, offsets, range, removed);
        changed();
    }

    /** Shift+Tab without a selection: removes up to {@code width} leading spaces from the cursor's line. */
    void dedentLine(int width) {
        int line = cursorLineCol()[0];
        int remove = leadingSpaces(getLines().get(line), width);
        if (remove == 0) return;
        int lineStart = lineOffsets()[line];
        this.undoManager.push(this.text.toString(), this.cursor);
        this.text.delete(lineStart, lineStart + remove);
        this.cursor = Math.max(lineStart, this.cursor - remove);
        this.selAnchor = this.cursor;
        changed();
    }

    /** Returns false when there is nothing to undo. */
    boolean undo() {
        return restore(this.undoManager.undo(this.text.toString(), this.cursor));
    }

    /** Returns false when there is nothing to redo. */
    boolean redo() {
        return restore(this.undoManager.redo(this.text.toString(), this.cursor));
    }

    // ---- internals ----

    private boolean restore(EditorUndoManager.EditorState state) {
        if (state == null) return false;
        this.text.setLength(0);
        this.text.append(state.text());
        this.cursor = Math.min(state.cursor(), this.text.length());
        this.selAnchor = this.cursor;
        changed();
        return true;
    }

    /** Deletes the selection as its own undo step; false when nothing was selected. */
    private boolean deleteSelection() {
        if (!hasSelection()) return false;
        this.undoManager.push(this.text.toString(), this.cursor);
        deleteSelectionNoUndo();
        changed();
        return true;
    }

    private void deleteSelectionNoUndo() {
        int lo = selectionStart();
        this.text.delete(lo, selectionEnd());
        this.cursor = lo;
        this.selAnchor = lo;
    }

    /** {first, last} line touched by the selection; a selection ending at a line's start leaves that line out. */
    private int[] selectedLineRange() {
        int first = lineColOf(selectionStart())[0];
        int[] end = lineColOf(selectionEnd());
        int last = end[1] == 0 && end[0] > first ? end[0] - 1 : end[0];
        return new int[]{first, last};
    }

    private int shiftForIndent(int pos, int[] offsets, int[] range, int width) {
        int shift = 0;
        for (int line = range[0]; line <= range[1] && offsets[line] < pos; line++) shift += width;
        return Math.min(pos + shift, this.text.length());
    }

    private static int shiftForDedent(int pos, int[] offsets, int[] range, int[] removed) {
        int removedBefore = 0;
        for (int line = range[0]; line <= range[1]; line++) {
            int r = removed[line - range[0]];
            if (pos < offsets[line]) break;
            if (r == 0) continue;
            if (pos < offsets[line] + r) return offsets[line] - removedBefore;
            removedBefore += r;
        }
        return Math.max(0, pos - removedBefore);
    }

    private static int leadingSpaces(String line, int max) {
        int n = 0;
        while (n < Math.min(max, line.length()) && line.charAt(n) == ' ') n++;
        return n;
    }

    private void changed() {
        invalidate();
        this.onTextChanged.run();
    }

    private void invalidate() {
        this.lineCache = null;
        this.offsetCache = null;
    }

    private int clamp(int pos) {
        return Math.max(0, Math.min(pos, this.text.length()));
    }

    static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
