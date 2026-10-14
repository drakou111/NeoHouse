package com.drakou111.neohouse.ui;

import java.util.ArrayList;
import java.util.List;

public class EditorUndoManager {

    public record EditorState(String text, int cursor) {}

    private static final int UNDO_LIMIT = 10000;
    private final List<EditorState> undoStack = new ArrayList<>();
    private final List<EditorState> redoStack = new ArrayList<>();

    public void push(String text, int cursor) {
        if (undoStack.size() >= UNDO_LIMIT) undoStack.removeFirst();
        undoStack.add(new EditorState(text, cursor));
        redoStack.clear();
    }

    public EditorState undo(String currentText, int currentCursor) {
        if (undoStack.isEmpty()) return null;
        EditorState e = undoStack.removeLast();
        redoStack.add(new EditorState(currentText, currentCursor));
        return e;
    }

    public EditorState redo(String currentText, int currentCursor) {
        if (redoStack.isEmpty()) return null;
        EditorState e = redoStack.removeLast();
        if (undoStack.size() >= UNDO_LIMIT) undoStack.removeFirst();
        undoStack.add(new EditorState(currentText, currentCursor));
        return e;
    }
}
