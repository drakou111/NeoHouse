package com.drakou111.neohouse.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EditorDocumentTest {

    private static final int TAB = 4;

    private int changes;
    private EditorDocument doc;

    @BeforeEach
    void setUp() {
        this.changes = 0;
        this.doc = new EditorDocument(() -> this.changes++);
    }

    private void load(String text, int cursor) {
        this.doc.setText(text);
        this.doc.moveCursor(cursor, false);
    }

    @Test
    void typingReplacesTheSelectionAndUndoRestoresIt() {
        load("hello world", 0);
        this.doc.select(6, 11);
        this.doc.insert("there");

        assertThat(this.doc.getText()).isEqualTo("hello there");
        assertThat(this.doc.getCursor()).isEqualTo(11);
        assertThat(this.changes).isEqualTo(1);

        assertThat(this.doc.undo()).isTrue();
        assertThat(this.doc.getText()).isEqualTo("hello world");
        assertThat(this.doc.redo()).isTrue();
        assertThat(this.doc.getText()).isEqualTo("hello there");
    }

    @Test
    void settingTheTextIsNotAnEdit() {
        load("abc", 0);

        assertThat(this.changes).isZero();
        assertThat(this.doc.undo()).isFalse();
    }

    @Test
    void newlineKeepsTheIndentation() {
        load("    foo();", 10);
        this.doc.insertNewline(true);

        assertThat(this.doc.getText()).isEqualTo("    foo();\n    ");
        assertThat(this.doc.cursorLineCol()).containsExactly(1, 4);
    }

    @Test
    void newlineWithoutAutoIndent() {
        load("    foo();", 10);
        this.doc.insertNewline(false);

        assertThat(this.doc.getText()).isEqualTo("    foo();\n");
    }

    @Test
    void deletingCharactersAndWords() {
        load("abc def", 7);
        this.doc.deleteWordBackward();
        assertThat(this.doc.getText()).isEqualTo("abc ");

        this.doc.deleteBackward();
        assertThat(this.doc.getText()).isEqualTo("abc");

        this.doc.moveCursor(0, false);
        this.doc.deleteForward();
        assertThat(this.doc.getText()).isEqualTo("bc");
        assertThat(this.doc.getCursor()).isZero();
    }

    @Test
    void wordMovementSkipsWordsAndSymbolRuns() {
        load("foo_bar  += baz", 0);

        assertThat(this.doc.wordRightOf(0)).isEqualTo(7);
        assertThat(this.doc.wordRightOf(7)).isEqualTo(11);
        assertThat(this.doc.wordLeftOf(15)).isEqualTo(12);
        assertThat(this.doc.wordLeftOf(12)).isEqualTo(9);
    }

    @Test
    void doubleClickSelectsTheIdentifier() {
        load("x = my_var + 1", 0);
        this.doc.selectWordAt(6);

        assertThat(this.doc.selectedText()).isEqualTo("my_var");
    }

    @Test
    void indentAndDedentSelectedLines() {
        load("a\nb\nc", 0);
        this.doc.select(0, 3); // "a\nb" (lines 0 and 1)
        this.doc.indentSelectedLines(TAB);

        assertThat(this.doc.getText()).isEqualTo("    a\n    b\nc");
        assertThat(this.doc.selectedText()).isEqualTo("    a\n    b");

        this.doc.selectAll();
        this.doc.dedentSelectedLines(TAB);
        assertThat(this.doc.getText()).isEqualTo("a\nb\nc");
    }

    @Test
    void selectionEndingAtALineStartLeavesThatLineAlone() {
        load("a\nb\n", 0);
        this.doc.select(0, 2); // ends at the start of line 1
        this.doc.indentSelectedLines(TAB);

        assertThat(this.doc.getText()).isEqualTo("    a\nb\n");
    }

    @Test
    void shiftTabDedentsTheCursorLine() {
        load("      x", 7);
        this.doc.dedentLine(TAB);

        assertThat(this.doc.getText()).isEqualTo("  x");
        assertThat(this.doc.getCursor()).isEqualTo(3);
    }

    @Test
    void cutWithoutSelectionTakesTheWholeLine() {
        load("one\ntwo\nthree", 5);
        String removed = this.doc.cutSelectionOrLine();

        assertThat(removed).isEqualTo("two\n");
        assertThat(this.doc.getText()).isEqualTo("one\nthree");
    }

    @Test
    void lineColumnLookup() {
        load("ab\n\ncde", 0);

        assertThat(this.doc.getLines()).containsExactly("ab", "", "cde");
        assertThat(this.doc.lineColOf(0)).containsExactly(0, 0);
        assertThat(this.doc.lineColOf(2)).containsExactly(0, 2);
        assertThat(this.doc.lineColOf(3)).containsExactly(1, 0);
        assertThat(this.doc.lineColOf(6)).containsExactly(2, 2);
        assertThat(this.doc.lineColOf(-3)).containsExactly(0, 0);
        assertThat(this.doc.lineColOf(100)).containsExactly(2, 3);
    }

    @Test
    void linesFollowEdits() {
        load("a", 1);
        assertThat(this.doc.getLines()).containsExactly("a");

        this.doc.insert("\nb");
        assertThat(this.doc.getLines()).containsExactly("a", "b");
        assertThat(this.doc.lineOffsets()).containsExactly(0, 2);
    }

    @Test
    void replaceAllKeepsTheCursorInRange() {
        load("aaaa", 4);
        this.doc.replaceAll("b");

        assertThat(this.doc.getText()).isEqualTo("b");
        assertThat(this.doc.getCursor()).isEqualTo(1);
    }
}
