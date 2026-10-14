package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.config.UITheme;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The code editor's Ctrl+F / Ctrl+R bar in its top-right corner: a find field, an optional replace
 * field with Replace / Replace All buttons, and the case-insensitive matches in the document.
 * Keys only go to the bar while one of its fields has focus; clicking the code gives the editor its
 * shortcuts back without closing the bar.
 */
final class FindReplaceBar {

    /** What the bar needs from the editor around it. */
    interface Host {
        void revealCursor();

        String getClipboard();

        void setClipboard(String text);
    }

    private enum Mode { CLOSED, FIND, REPLACE }

    private enum Focus { FIND, REPLACE, CODE }

    static final int WIDTH = 260;
    private static final int FIELD_H = 12;
    private static final int BUTTON_W = 60;
    private static final int REPLACE_ALL_EXTRA_W = 10;
    private static final int BUTTON_H = 11;
    private static final int BLINK_MS = 530;

    private final EditorDocument document;
    private final Host host;
    private final Field query = new Field();
    private final Field replacement = new Field();
    private final List<Integer> matches = new ArrayList<>();
    private Mode mode = Mode.CLOSED;
    private Focus focus = Focus.FIND;
    private int matchIndex = -1;

    FindReplaceBar(EditorDocument document, Host host) {
        this.document = document;
        this.host = host;
    }

    boolean isOpen() {
        return this.mode != Mode.CLOSED;
    }

    /** Whether one of the bar's fields has focus (and so takes the keyboard). */
    boolean hasFocus() {
        return isOpen() && this.focus != Focus.CODE;
    }

    void openFind() {
        open(Mode.FIND);
    }

    void openReplace() {
        open(Mode.REPLACE);
    }

    void close() {
        this.mode = Mode.CLOSED;
        this.matches.clear();
        this.matchIndex = -1;
    }

    /** The code was clicked: keys go back to the editor, the bar stays open. */
    void focusCode() {
        this.focus = Focus.CODE;
    }

    /** Offsets of the current matches; each is {@link #queryLength()} long. */
    List<Integer> getMatches() {
        return this.matches;
    }

    int getMatchIndex() {
        return this.matchIndex;
    }

    int queryLength() {
        return this.query.value.length();
    }

    /** Call after every edit so the matches follow the text. */
    void onTextChanged() {
        if (isOpen()) updateMatches();
    }

    // ---- keyboard ----

    boolean keyPressed(int key, int mods) {
        boolean shift = (mods & Keys.MOD_SHIFT) != 0;
        boolean ctrl = (mods & Keys.MOD_CONTROL) != 0;

        if (key == Keys.ESCAPE) { close(); return true; }
        if (key == Keys.ENTER || key == Keys.KP_ENTER) {
            if (this.focus == Focus.REPLACE) replaceOne();
            else if (shift) step(-1);
            else step(1);
            return true;
        }
        if (key == Keys.TAB && this.mode == Mode.REPLACE) {
            this.focus = this.focus == Focus.FIND ? Focus.REPLACE : Focus.FIND;
            return true;
        }
        if (ctrl) {
            switch (key) {
                case Keys.H -> { replaceAll(); return true; }
                case Keys.F -> { this.focus = Focus.FIND; return true; }
                case Keys.R -> { this.mode = Mode.REPLACE; this.focus = Focus.REPLACE; return true; }
                case Keys.C -> { copyField(); return true; }
                case Keys.X -> { copyField(); edited(focused().deleteSelection()); return true; }
                case Keys.V -> { paste(); return true; }
                case Keys.A -> { focused().selectAll(); return true; }
                default -> { }
            }
        }

        Field field = focused();
        switch (key) {
            case Keys.BACKSPACE -> edited(field.deleteBackward());
            case Keys.DELETE -> edited(field.deleteForward());
            case Keys.LEFT -> field.moveHorizontal(-1, ctrl, shift);
            case Keys.RIGHT -> field.moveHorizontal(1, ctrl, shift);
            case Keys.HOME -> field.moveTo(0, shift);
            case Keys.END -> field.moveTo(field.value.length(), shift);
            default -> { return false; }
        }
        return true;
    }

    boolean charTyped(char c) {
        if (c < 32 || c == 127) return false;
        focused().insert(String.valueOf(c));
        edited(true);
        return true;
    }

    // ---- mouse ----

    /** Handles a left click at ({@code mx}, {@code my}) for a bar drawn at {@code barX}, {@code barY}; true when it hit the bar. */
    boolean mouseClicked(Font font, double mx, double my, int barX, int barY) {
        if (!isOpen()) return false;
        int fieldW = fieldWidth(font);
        int fx = barX + 4;
        int fy = barY + 4;
        if (this.mode == Mode.REPLACE) {
            int ry = fy + FIELD_H + 4;
            int by = ry + FIELD_H + 3;
            if (inside(mx, my, fx, by, BUTTON_W, BUTTON_H)) { replaceOne(); return true; }
            if (inside(mx, my, fx + BUTTON_W + 3, by, BUTTON_W + REPLACE_ALL_EXTRA_W, BUTTON_H)) { replaceAll(); return true; }
            if (inside(mx, my, fx, ry, fieldW, FIELD_H)) {
                this.focus = Focus.REPLACE;
                this.replacement.clickAt(font, (int) (mx - fx - 2));
                return true;
            }
        }
        if (inside(mx, my, fx, fy, fieldW, FIELD_H)) {
            this.focus = Focus.FIND;
            this.query.clickAt(font, (int) (mx - fx - 2));
            return true;
        }
        return false;
    }

    // ---- drawing ----

    void render(Gfx g, Font font, UITheme t, int barX, int barY) {
        if (!isOpen()) return;
        boolean replacing = this.mode == Mode.REPLACE;
        int rows = replacing ? 2 : 1;
        int barH = 6 + rows * (FIELD_H + 4) + (replacing ? 16 : 0);

        g.fill(barX, barY, barX + WIDTH, barY + barH, t.dialog(0xF2));
        g.fill(barX, barY, barX + WIDTH, barY + 1, t.accent());
        g.fill(barX, barY, barX + 1, barY + barH, t.accent());
        g.fill(barX + WIDTH - 1, barY, barX + WIDTH, barY + barH, t.accent());
        g.fill(barX, barY + barH - 1, barX + WIDTH, barY + barH, t.accent());

        String count = countLabel();
        int fieldW = fieldWidth(font);
        int fy = barY + 4;
        this.query.render(g, font, t, barX + 4, fy, fieldW, this.focus == Focus.FIND, "Find");
        if (!count.isEmpty()) {
            int color = this.matches.isEmpty() ? 0xFFFF6666 : 0xFF88CC88;
            g.drawString(font, count, barX + WIDTH - (font.width(count) + 4) - 2, fy + 2, color, false);
        }
        if (!replacing) return;

        int ry = fy + FIELD_H + 4;
        this.replacement.render(g, font, t, barX + 4, ry, fieldW, this.focus == Focus.REPLACE, "Replace");
        int by = ry + FIELD_H + 3;
        renderButton(g, font, t, barX + 4, by, BUTTON_W, "Replace");
        renderButton(g, font, t, barX + 4 + BUTTON_W + 3, by, BUTTON_W + REPLACE_ALL_EXTRA_W, "Replace All");
    }

    // ---- matching (pure, for tests) ----

    /** Start offsets of every case-insensitive occurrence of {@code query}, overlapping ones included. */
    static List<Integer> findMatches(String text, String query) {
        List<Integer> found = new ArrayList<>();
        if (query.isEmpty()) return found;
        String src = text.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        int at = src.indexOf(q);
        while (at >= 0) {
            found.add(at);
            at = src.indexOf(q, at + 1);
        }
        return found;
    }

    /** {@code text} with every case-insensitive, non-overlapping occurrence of {@code query} replaced. */
    static String replaceAllIgnoringCase(String text, String query, String replacement) {
        if (query.isEmpty()) return text;
        String src = text.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(text.length());
        int from = 0;
        int at = src.indexOf(q);
        while (at >= 0) {
            out.append(text, from, at).append(replacement);
            from = at + q.length();
            at = src.indexOf(q, from);
        }
        return out.append(text, from, text.length()).toString();
    }

    // ---- internals ----

    private void open(Mode newMode) {
        this.mode = newMode;
        this.focus = Focus.FIND;
        this.query.select(0, this.query.value.length());
        updateMatches();
    }

    private Field focused() {
        return this.focus == Focus.REPLACE ? this.replacement : this.query;
    }

    /** After an edit of a field: the matches change only when it was the query. */
    private void edited(boolean changed) {
        if (changed && this.focus != Focus.REPLACE) updateMatches();
    }

    private void updateMatches() {
        this.matches.clear();
        this.matches.addAll(findMatches(this.document.getText(), this.query.value.toString()));
        this.matchIndex = -1;
        if (this.matches.isEmpty()) return;
        this.matchIndex = 0;
        for (int k = 0; k < this.matches.size(); k++) {
            if (this.matches.get(k) >= this.document.getCursor()) {
                this.matchIndex = k;
                break;
            }
        }
    }

    private void step(int direction) {
        if (this.matches.isEmpty()) return;
        this.matchIndex = Math.floorMod(this.matchIndex + direction, this.matches.size());
        jumpToMatch();
    }

    private void jumpToMatch() {
        if (this.matchIndex < 0 || this.matchIndex >= this.matches.size()) return;
        int offset = this.matches.get(this.matchIndex);
        this.document.select(offset, offset + queryLength());
        this.host.revealCursor();
    }

    private void replaceOne() {
        if (this.matchIndex < 0 || this.matchIndex >= this.matches.size()) return;
        int offset = this.matches.get(this.matchIndex);
        if (offset + queryLength() > this.document.length()) return;
        // The cursor ends after the replacement, so the matches restart from the next one after it.
        this.document.replace(offset, offset + queryLength(), this.replacement.value.toString());
        updateMatches();
        jumpToMatch();
    }

    private void replaceAll() {
        if (this.query.value.isEmpty()) return;
        updateMatches();
        if (this.matches.isEmpty()) return;
        this.document.replaceAll(replaceAllIgnoringCase(this.document.getText(), this.query.value.toString(),
                this.replacement.value.toString()));
        updateMatches();
    }

    private void copyField() {
        Field field = focused();
        if (field.hasSelection()) this.host.setClipboard(field.selectedText());
    }

    private void paste() {
        String clip = this.host.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        focused().insert(clip);
        edited(true);
    }

    private String countLabel() {
        if (this.query.value.isEmpty()) return "";
        return this.matches.isEmpty() ? "no matches" : (this.matchIndex + 1) + "/" + this.matches.size();
    }

    private int fieldWidth(Font font) {
        String count = countLabel();
        int labelW = count.isEmpty() ? 0 : font.width(count) + 4;
        return WIDTH - 12 - labelW;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void renderButton(Gfx g, Font font, UITheme t, int bx, int by, int bw, String label) {
        g.fill(bx, by, bx + bw, by + BUTTON_H, t.panel(0xFF));
        g.fill(bx, by, bx + bw, by + 1, t.accent());
        g.fill(bx, by, bx + 1, by + BUTTON_H, t.accent());
        g.fill(bx + bw - 1, by, bx + bw, by + BUTTON_H, t.accent());
        g.fill(bx, by + BUTTON_H - 1, bx + bw, by + BUTTON_H, t.accent());
        g.drawString(font, label, bx + (bw - font.width(label)) / 2, by + 2, 0xFFCCCCCC, false);
    }

    /** One single-line text field of the bar, with its own cursor and selection. */
    private static final class Field {
        private final StringBuilder value = new StringBuilder();
        private int cursor = 0;
        private int anchor = 0;

        boolean hasSelection() {
            return this.cursor != this.anchor;
        }

        String selectedText() {
            return this.value.substring(Math.min(this.cursor, this.anchor), Math.max(this.cursor, this.anchor));
        }

        void select(int anchor, int cursor) {
            this.anchor = anchor;
            this.cursor = cursor;
        }

        void selectAll() {
            select(0, this.value.length());
        }

        void moveTo(int pos, boolean extend) {
            this.cursor = pos;
            if (!extend) this.anchor = pos;
        }

        void moveHorizontal(int direction, boolean byWord, boolean extend) {
            if (!extend && hasSelection()) {
                moveTo(direction < 0 ? Math.min(this.cursor, this.anchor) : Math.max(this.cursor, this.anchor), false);
                return;
            }
            int target;
            if (byWord) target = direction < 0 ? wordLeft(this.cursor) : wordRight(this.cursor);
            else target = Math.max(0, Math.min(this.value.length(), this.cursor + direction));
            moveTo(target, extend);
        }

        void insert(String s) {
            deleteSelection();
            this.value.insert(this.cursor, s);
            moveTo(this.cursor + s.length(), false);
        }

        /** Returns whether anything changed. */
        boolean deleteSelection() {
            if (!hasSelection()) return false;
            int lo = Math.min(this.cursor, this.anchor);
            this.value.delete(lo, Math.max(this.cursor, this.anchor));
            moveTo(lo, false);
            return true;
        }

        boolean deleteBackward() {
            if (deleteSelection()) return true;
            if (this.cursor == 0) return false;
            this.value.deleteCharAt(this.cursor - 1);
            moveTo(this.cursor - 1, false);
            return true;
        }

        boolean deleteForward() {
            if (deleteSelection()) return true;
            if (this.cursor >= this.value.length()) return false;
            this.value.deleteCharAt(this.cursor);
            return true;
        }

        /** Places the cursor at the character nearest to {@code relX} pixels into the visible text. */
        void clickAt(Font font, int relX) {
            String s = this.value.toString();
            int target = relX + scrollOffset(font, s, this.cursor, WIDTH - 16);
            int px = 0;
            int col = s.length();
            for (int i = 0; i < s.length(); i++) {
                int cw = font.width(String.valueOf(s.charAt(i)));
                if (px + (cw + 1) / 2 >= target) {
                    col = i;
                    break;
                }
                px += cw;
            }
            moveTo(col, false);
        }

        void render(Gfx g, Font font, UITheme t, int fx, int fy, int fieldW, boolean focused, String placeholder) {
            g.fill(fx, fy, fx + fieldW, fy + FIELD_H, focused ? 0xFF2A2A3A : 0xFF222222);
            g.fill(fx, fy, fx + fieldW, fy + 1, focused ? t.accent() : t.divider());

            String display = this.value.toString();
            if (display.isEmpty()) {
                g.drawString(font, placeholder, fx + 2, fy + 2, 0xFF555555, false);
                return;
            }

            int scroll = scrollOffset(font, display, this.cursor, fieldW - 4);
            g.enableScissor(fx, fy, fx + fieldW, fy + FIELD_H);
            if (focused && hasSelection()) {
                int sx = fx + 2 + font.width(display.substring(0, Math.min(this.cursor, this.anchor))) - scroll;
                int ex = fx + 2 + font.width(display.substring(0, Math.max(this.cursor, this.anchor))) - scroll;
                g.fill(sx, fy + 1, ex, fy + FIELD_H - 1, 0x664477AA);
            }
            g.drawString(font, display, fx + 2 - scroll, fy + 2, 0xFFDDDDDD, false);
            if (focused && (System.currentTimeMillis() / BLINK_MS) % 2 == 0) {
                int cx = fx + 2 + font.width(display.substring(0, this.cursor)) - scroll;
                g.fill(cx, fy + 1, cx + 1, fy + FIELD_H - 1, 0xFFCCCCCC);
            }
            g.disableScissor();
        }

        /** How far the text is scrolled left so the cursor stays inside {@code visibleW}. */
        private static int scrollOffset(Font font, String s, int cursor, int visibleW) {
            return Math.max(0, font.width(s.substring(0, Math.min(cursor, s.length()))) - visibleW + 4);
        }

        private int wordLeft(int pos) {
            if (pos <= 0) return 0;
            int p = pos - 1;
            while (p > 0 && !EditorDocument.isIdentifierChar(this.value.charAt(p - 1))) p--;
            while (p > 0 && EditorDocument.isIdentifierChar(this.value.charAt(p - 1))) p--;
            return p;
        }

        private int wordRight(int pos) {
            int len = this.value.length();
            int p = pos;
            while (p < len && !EditorDocument.isIdentifierChar(this.value.charAt(p))) p++;
            while (p < len && EditorDocument.isIdentifierChar(this.value.charAt(p))) p++;
            return p;
        }
    }
}
