package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.constants.AddAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SyntaxEditorWidget extends AbstractWidget {

    // ── Text state ────────────────────────────────────────────────────────────
    private final StringBuilder text = new StringBuilder();
    private int cursor    = 0;
    private int selAnchor = 0;
    private int scrollLine = 0;

    // ── Error state ───────────────────────────────────────────────────────────
    private int    errorLine     = -1;   // 0-based
    private int    errorColStart = -1;   // 0-based column of token start in line
    private int    errorTokenLen = 0;
    private String errorMsg      = null;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Runnable onChange;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LINE_H         = 10;
    private static final int GUTTER_W       = 30;
    private static final int PAD_X          = 4;
    private static final int PAD_Y          = 2;
    private static final int ERROR_BAR_H    = 14;
    private static final int SPACES_PER_TAB = 4;

    // ── Highlight spans ───────────────────────────────────────────────────────
    private record Span(int start, int end, int color) {}
    private final List<Span> spans = new ArrayList<>();

    // ── Undo / Redo ───────────────────────────────────────────────────────────
    private record UndoEntry(String text, int cursor) {}
    private final List<UndoEntry> undoStack = new ArrayList<>();
    private final List<UndoEntry> redoStack = new ArrayList<>();
    private static final int UNDO_LIMIT = 200;

    public SyntaxEditorWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
    }

    public void setOnChange(Runnable r) { this.onChange = r; }

    public String getValue() { return text.toString(); }

    public void setValue(String s) {
        text.setLength(0);
        String normalized = (s == null ? "" : s)
                .replace("\r\n", "\n").replace("\r", "\n")
                .replace("\t", " ".repeat(SPACES_PER_TAB));
        text.append(normalized);
        cursor    = 0;
        selAnchor = 0;
        scrollLine = 0;
        rebuildSpans();
    }

    public void setError(int line0, int colStart0, int tokenLen, String msg) {
        this.errorLine     = line0;
        this.errorColStart = colStart0;
        this.errorTokenLen = tokenLen;
        this.errorMsg      = msg;
    }

    public void clearError() {
        this.errorLine     = -1;
        this.errorColStart = -1;
        this.errorTokenLen = 0;
        this.errorMsg      = null;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        Font font  = Minecraft.getInstance().font;
        int  x0    = getX(), y0 = getY();
        int  codeH = height - (errorMsg != null ? ERROR_BAR_H : 0);

        g.fill(x0,             y0, x0 + width,      y0 + codeH, 0xCC1E1E1E);
        g.fill(x0,             y0, x0 + GUTTER_W,   y0 + codeH, 0xCC252526);
        g.fill(x0 + GUTTER_W, y0, x0 + GUTTER_W + 1, y0 + codeH, 0xFF3C3C3C);

        List<String> lines    = getLines();
        int          visLines = Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
        scrollLine = clampScrollLine(scrollLine, lines.size(), visLines);
        int[] lineOffsets = buildLineOffsets(lines);

        int[] curLC  = offsetToLC(cursor,    lines);
        boolean hasSel = cursor != selAnchor;

        int endLine = Math.min(scrollLine + visLines, lines.size());
        for (int li = scrollLine; li < endLine; li++) {
            int    ry        = y0 + PAD_Y + (li - scrollLine) * LINE_H;
            String line      = lines.get(li);
            int    lineStart = lineOffsets[li];

            // Error-line background tint
            if (li == errorLine) {
                g.fill(x0 + GUTTER_W + 1, ry - 1, x0 + width, ry + LINE_H - 1, 0x44FF3333);
            }

            // Selection
            if (hasSel) {
                int selLo = Math.min(cursor, selAnchor);
                int selHi = Math.max(cursor, selAnchor);
                int rowEnd = lineStart + line.length();
                int hi0 = Math.max(selLo, lineStart);
                int hi1 = Math.min(selHi, rowEnd);
                if (hi0 < hi1) {
                    int sx = codeX() + font.width(line.substring(0, hi0 - lineStart));
                    int ex = codeX() + font.width(line.substring(0, hi1 - lineStart));
                    g.fill(sx, ry - 1, ex, ry + LINE_H - 1, 0x664477AA);
                }
            }

            // Gutter line number
            String num = String.valueOf(li + 1);
            g.drawString(font, num,
                    x0 + GUTTER_W - font.width(num) - 3, ry,
                    li == errorLine ? 0xFFFF6666 : 0xFF636363, false);

            // Code
            renderHighlightedLine(g, font, line, lineStart, codeX(), ry);

            // Red wavy underline for error token
            if (li == errorLine && errorColStart >= 0 && errorTokenLen > 0) {
                int uLo = Math.min(errorColStart, line.length());
                int uHi = Math.min(errorColStart + errorTokenLen, line.length());
                if (uHi > uLo) {
                    int ux0 = codeX() + font.width(line.substring(0, uLo));
                    int ux1 = codeX() + font.width(line.substring(0, uHi));
                    int uy  = ry + LINE_H - 2;
                    for (int wx = ux0; wx < ux1; wx++) {
                        g.fill(wx, uy + (wx % 2), wx + 1, uy + (wx % 2) + 1, 0xFFFF4444);
                    }
                }
            }

            // Cursor
            if (isFocused() && li == curLC[0]) {
                int col = Math.min(curLC[1], line.length());
                int cx  = codeX() + font.width(line.substring(0, col));
                if ((System.currentTimeMillis() / 530) % 2 == 0) {
                    g.fill(cx, ry - 1, cx + 1, ry + LINE_H - 1, 0xFFCCCCCC);
                }
            }
        }

        // Error bar
        if (errorMsg != null) {
            int ey = y0 + codeH;
            g.fill(x0, ey,     x0 + width, ey + ERROR_BAR_H, 0xDD3B1111);
            g.fill(x0, ey,     x0 + width, ey + 1,           0xFFAA3333);
            String display = firstMeaningfulLine(errorMsg);
            if (display.length() > 120) display = display.substring(0, 117) + "...";
            g.drawString(font, display, x0 + 4, ey + 3, 0xFFFF8888, false);
        }

        // Border
        g.fill(x0,             y0,              x0 + width,     y0 + 1,         0xFF3C3C4C);
        g.fill(x0,             y0 + height - 1, x0 + width,     y0 + height,    0xFF3C3C4C);
        g.fill(x0,             y0,              x0 + 1,         y0 + height,    0xFF3C3C4C);
        g.fill(x0 + width - 1, y0,              x0 + width,     y0 + height,    0xFF3C3C4C);
    }

    private int codeX() { return getX() + GUTTER_W + 1 + PAD_X; }

    private static String firstMeaningfulLine(String s) {
        for (String part : s.split("\n")) {
            String t = part.trim();
            if (!t.isEmpty()) return t;
        }
        return s.trim();
    }

    private void renderHighlightedLine(GuiGraphics g, Font font,
                                       String line, int lineStart,
                                       int x, int y) {
        if (line.isEmpty()) return;
        int lineEnd = lineStart + line.length();
        int drawn   = 0;

        for (Span sp : spans) {
            if (sp.end() <= lineStart) continue;
            if (sp.start() >= lineEnd)  break;

            int lo = Math.max(0, Math.min(Math.max(sp.start(), lineStart) - lineStart, line.length()));
            int hi = Math.max(lo, Math.min(Math.min(sp.end(), lineEnd) - lineStart, line.length()));

            if (lo > drawn) {
                String gap = line.substring(drawn, lo);
                g.drawString(font, gap, x, y, 0xFFD4D4D4, false);
                x += font.width(gap);
            }
            if (hi > lo) {
                String seg   = line.substring(lo, hi);
                int    color = sp.color() == 0 ? 0xFFD4D4D4 : sp.color();
                g.drawString(font, seg, x, y, color, false);
                x += font.width(seg);
            }
            drawn = hi;
        }

        if (drawn < line.length()) {
            g.drawString(font, line.substring(drawn), x, y, 0xFFD4D4D4, false);
        }
    }

    // ── Span builder ──────────────────────────────────────────────────────────

    private void rebuildSpans() {
        spans.clear();
        String src = text.toString();
        int    pos = 0;
        while (pos < src.length()) {
            String  rest    = src.substring(pos);
            boolean matched = false;
            for (TokenType tt : TokenType.values()) {
                if (tt.pattern == null) continue;
                Matcher m = tt.pattern.matcher(rest);
                if (!m.lookingAt()) continue;
                int len   = m.group().length();
                int color = tokenColor(tt, rest.substring(0, len));
                spans.add(new Span(pos, pos + len, color));
                pos    += len;
                matched = true;
                break;
            }
            if (!matched) pos++;
        }
    }

    private static int tokenColor(TokenType t, String text) {
        return switch (t) {
            case FUNCTION, RETURN, CANCEL, LOOP, GLOBAL,
                 IF, ELSE, REPEAT, EVENT, DEFINE, RANDOM -> 0xFF569CD6;
            case LONG, DOUBLE, STRING, BOOL, VAR          -> 0xFF4EC9B0;
            case TRUE, FALSE                               -> 0xFF569CD6;
            case LONG_CAST, DOUBLE_CAST, STRING_CAST       -> 0xFF4EC9B0;
            case LONG_VALUE, DOUBLE_VALUE                  -> 0xFFB5CEA8;
            case STRING_VALUE                              -> 0xFFCE9178;
            case COMMENT_LINE, COMMENT_BLOCK               -> 0xFF6A9955;
            case NOTE                                      -> 0xFF608B4E;
            case CONST_IDENT                               -> 0xFF4FC1FF;
            case IDENT -> AddAction.searchByName(text) != null ? 0xFFDCDCAA : 0xFFD4D4D4;
            case WHITESPACE                                -> 0; // sentinel
            default                                        -> 0xFFD4D4D4;
        };
    }

    // ── Keyboard input ────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl  = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT)   != 0;

        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> { selectAll();      return true; }
                case GLFW.GLFW_KEY_C -> { copySelection();  return true; }
                case GLFW.GLFW_KEY_X -> { cutSelection();   return true; }
                case GLFW.GLFW_KEY_V -> { pasteClipboard(); return true; }
                case GLFW.GLFW_KEY_Z -> { if (shift) redo(); else undo(); return true; }
                case GLFW.GLFW_KEY_Y -> { redo();           return true; }
                case GLFW.GLFW_KEY_HOME -> { moveCursor(0,            shift); return true; }
                case GLFW.GLFW_KEY_END  -> { moveCursor(text.length(), shift); return true; }
            }
        }

        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                if (shift) dedent(); else insertString(" ".repeat(SPACES_PER_TAB));
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBackward(); return true; }
            case GLFW.GLFW_KEY_DELETE    -> { deleteForward();  return true; }
            case GLFW.GLFW_KEY_ENTER,
                 GLFW.GLFW_KEY_KP_ENTER  -> { insertChar('\n'); return true; }
            case GLFW.GLFW_KEY_LEFT      -> { moveHoriz(-1, shift); return true; }
            case GLFW.GLFW_KEY_RIGHT     -> { moveHoriz( 1, shift); return true; }
            case GLFW.GLFW_KEY_UP        -> { moveVert(-1, shift);  return true; }
            case GLFW.GLFW_KEY_DOWN      -> { moveVert( 1, shift);  return true; }
            case GLFW.GLFW_KEY_HOME      -> { moveToLineEdge(true,  shift); return true; }
            case GLFW.GLFW_KEY_END       -> { moveToLineEdge(false, shift); return true; }
            case GLFW.GLFW_KEY_PAGE_UP   -> { moveVert(-visibleLines(), shift); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { moveVert( visibleLines(), shift); return true; }
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (c < 32 || c == 127) return false;
        insertChar(c);
        return true;
    }

    // ── Mouse input ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) { setFocused(false); return false; }
        setFocused(true);
        int pos = hitToOffset((int) mx, (int) my);
        if (pos >= 0) {
            cursor    = pos;
            selAnchor = pos;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn != 0 || !isFocused()) return false;
        int pos = hitToOffset((int) mx, (int) my);
        if (pos >= 0) {
            cursor = Math.max(0, Math.min(pos, text.length()));
            // selAnchor stays fixed at where the click started
            ensureCursorVisible();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!isMouseOver(mx, my)) return false;
        scrollLine = clampScrollLine(scrollLine - (int) Math.signum(dy),
                getLines().size(), visibleLines());
        return true;
    }

    // ── Text mutation ─────────────────────────────────────────────────────────

    private void insertChar(char c) {
        pushUndo();
        if (hasSelection()) deleteSelectionInternal();
        text.insert(cursor, c);
        cursor++;
        selAnchor = cursor;
        onTextChanged();
    }

    private void insertString(String s) {
        if (s.isEmpty()) return;
        pushUndo();
        if (hasSelection()) deleteSelectionInternal();
        text.insert(cursor, s);
        cursor   += s.length();
        selAnchor = cursor;
        onTextChanged();
    }

    private void dedent() {
        List<String> lines = getLines();
        int[] lc   = offsetToLC(cursor, lines);
        int[] offs = buildLineOffsets(lines);
        String line = lines.get(lc[0]);
        int remove = 0;
        for (int i = 0; i < Math.min(SPACES_PER_TAB, line.length()); i++) {
            if (line.charAt(i) == ' ') remove++;
            else break;
        }
        if (remove == 0) return;
        pushUndo();
        int lineStart = offs[lc[0]];
        text.delete(lineStart, lineStart + remove);
        cursor    = Math.max(lineStart, cursor - remove);
        selAnchor = cursor;
        onTextChanged();
    }

    private void deleteBackward() {
        if (hasSelection()) { pushUndo(); deleteSelectionInternal(); onTextChanged(); return; }
        if (cursor <= 0) return;
        pushUndo();
        text.deleteCharAt(cursor - 1);
        cursor--;
        selAnchor = cursor;
        onTextChanged();
    }

    private void deleteForward() {
        if (hasSelection()) { pushUndo(); deleteSelectionInternal(); onTextChanged(); return; }
        if (cursor >= text.length()) return;
        pushUndo();
        text.deleteCharAt(cursor);
        onTextChanged();
    }

    private void deleteSelectionInternal() {
        int lo = Math.min(cursor, selAnchor);
        int hi = Math.max(cursor, selAnchor);
        text.delete(lo, hi);
        cursor    = lo;
        selAnchor = lo;
    }

    private boolean hasSelection() { return cursor != selAnchor; }

    private void onTextChanged() {
        rebuildSpans();
        if (onChange != null) onChange.run();
    }

    // ── Cursor movement ───────────────────────────────────────────────────────

    private void moveCursor(int pos, boolean select) {
        pos    = Math.max(0, Math.min(pos, text.length()));
        cursor = pos;
        if (!select) selAnchor = pos;
        ensureCursorVisible();
    }

    private void moveHoriz(int dir, boolean select) {
        if (!select && hasSelection()) {
            cursor    = dir < 0 ? Math.min(cursor, selAnchor) : Math.max(cursor, selAnchor);
            selAnchor = cursor;
            ensureCursorVisible();
            return;
        }
        moveCursor(cursor + dir, select);
    }

    private void moveVert(int lineDelta, boolean select) {
        List<String> lines = getLines();
        int[] lc       = offsetToLC(cursor, lines);
        int   target   = Math.max(0, Math.min(lc[0] + lineDelta, lines.size() - 1));
        int   col      = Math.min(lc[1], lines.get(target).length());
        int[] offs     = buildLineOffsets(lines);
        moveCursor(offs[target] + col, select);
    }

    private void moveToLineEdge(boolean home, boolean select) {
        List<String> lines = getLines();
        int[] lc   = offsetToLC(cursor, lines);
        int[] offs = buildLineOffsets(lines);
        int pos    = home ? offs[lc[0]] : offs[lc[0]] + lines.get(lc[0]).length();
        moveCursor(pos, select);
    }

    private void selectAll() {
        selAnchor = 0;
        cursor    = text.length();
    }

    // ── Clipboard ─────────────────────────────────────────────────────────────

    private void copySelection() {
        if (!hasSelection()) return;
        int lo = Math.min(cursor, selAnchor);
        int hi = Math.max(cursor, selAnchor);
        Minecraft.getInstance().keyboardHandler.setClipboard(text.substring(lo, hi));
    }

    private void cutSelection() {
        if (!hasSelection()) return;
        copySelection();
        pushUndo();
        deleteSelectionInternal();
        onTextChanged();
    }

    private void pasteClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        clip = clip.replace("\r\n", "\n").replace("\r", "\n")
                   .replace("\t", " ".repeat(SPACES_PER_TAB));
        insertString(clip);
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    private void pushUndo() {
        if (undoStack.size() >= UNDO_LIMIT) undoStack.remove(0);
        undoStack.add(new UndoEntry(text.toString(), cursor));
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        UndoEntry e = undoStack.remove(undoStack.size() - 1);
        redoStack.add(new UndoEntry(text.toString(), cursor));
        text.setLength(0);
        text.append(e.text());
        cursor    = Math.min(e.cursor(), text.length());
        selAnchor = cursor;
        onTextChanged();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        UndoEntry e = redoStack.remove(redoStack.size() - 1);
        if (undoStack.size() >= UNDO_LIMIT) undoStack.remove(0);
        undoStack.add(new UndoEntry(text.toString(), cursor));
        text.setLength(0);
        text.append(e.text());
        cursor    = Math.min(e.cursor(), text.length());
        selAnchor = cursor;
        onTextChanged();
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    private void ensureCursorVisible() {
        List<String> lines = getLines();
        int[] lc  = offsetToLC(cursor, lines);
        int   vis = visibleLines();
        if (lc[0] < scrollLine)        scrollLine = lc[0];
        if (lc[0] >= scrollLine + vis) scrollLine = lc[0] - vis + 1;
        scrollLine = clampScrollLine(scrollLine, lines.size(), vis);
    }

    private int clampScrollLine(int s, int lineCount, int vis) {
        return Math.max(0, Math.min(s, Math.max(0, lineCount - vis)));
    }

    private int visibleLines() {
        int codeH = height - (errorMsg != null ? ERROR_BAR_H : 0);
        return Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
    }

    // ── Hit testing ───────────────────────────────────────────────────────────

    private int hitToOffset(int mx, int my) {
        Font         font  = Minecraft.getInstance().font;
        List<String> lines = getLines();
        int[]        offs  = buildLineOffsets(lines);
        int li = scrollLine + (my - (getY() + PAD_Y)) / LINE_H;
        li = Math.max(0, Math.min(li, lines.size() - 1));
        String line = lines.get(li);
        int relX = mx - codeX();
        int col  = 0;
        while (col < line.length()) {
            int w = font.width(line.charAt(col) + "");
            if (relX < w / 2) break;
            relX -= w;
            col++;
        }
        return offs[li] + col;
    }

    // ── Line / offset utilities ───────────────────────────────────────────────

    private List<String> getLines() {
        List<String> out   = new ArrayList<>();
        String       s     = text.toString();
        int          start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '\n') {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    private int[] buildLineOffsets(List<String> lines) {
        int[] offs = new int[lines.size()];
        int   acc  = 0;
        for (int i = 0; i < lines.size(); i++) {
            offs[i] = acc;
            acc    += lines.get(i).length() + 1;
        }
        return offs;
    }

    private int[] offsetToLC(int offset, List<String> lines) {
        int[] offs = buildLineOffsets(lines);
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (offset >= offs[i]) {
                return new int[]{i, Math.min(offset - offs[i], lines.get(i).length())};
            }
        }
        return new int[]{0, 0};
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}
