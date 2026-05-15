package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.List;

public class EditorUndoManager {

    public record EditorState(String text, int cursor) {}

    private static final int UNDO_LIMIT = 200;
    private final List<EditorState> undoStack = new ArrayList<>();
    private final List<EditorState> redoStack = new ArrayList<>();

    public void push(String text, int cursor) {
        if (undoStack.size() >= UNDO_LIMIT) undoStack.remove(0);
        undoStack.add(new EditorState(text, cursor));
        redoStack.clear();
    }

    /** Returns the state to restore, or null if nothing to undo. */
    public EditorState undo(String currentText, int currentCursor) {
        if (undoStack.isEmpty()) return null;
        EditorState e = undoStack.remove(undoStack.size() - 1);
        redoStack.add(new EditorState(currentText, currentCursor));
        return e;
    }

    /** Returns the state to restore, or null if nothing to redo. */
    public EditorState redo(String currentText, int currentCursor) {
        if (redoStack.isEmpty()) return null;
        EditorState e = redoStack.remove(redoStack.size() - 1);
        if (undoStack.size() >= UNDO_LIMIT) undoStack.remove(0);
        undoStack.add(new EditorState(currentText, currentCursor));
        return e;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}
