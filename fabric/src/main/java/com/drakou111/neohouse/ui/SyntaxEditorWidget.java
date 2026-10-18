package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.WidgetBase;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.ui.ParameterHints.CallAt;
import com.drakou111.neohouse.ui.ParameterHints.InlineHint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The script code editor: draws the text with syntax colours, inline parameter hints, the
 * selection, find matches and error underlines, and routes keyboard and mouse input. The text and
 * its edits live in {@link EditorDocument}; hints in {@link ParameterHints}; completions in
 * {@link EditorSuggestions}; Ctrl+F / Ctrl+R in {@link FindReplaceBar}.
 */
public class SyntaxEditorWidget extends WidgetBase {

    private static final int LINE_H = 10;
    private static final int GUTTER_W = 30;
    private static final int PAD_X = 4;
    private static final int PAD_Y = 2;
    private static final int ERROR_BAR_H_MIN = 14;
    private static final int SB_W = 4;
    private static final int SB_PAD = 2;
    private static final int HSB_H = 4;
    private static final int HSB_MIN_THUMB_W = 12;
    private static final int H_SCROLL_STEP = 20;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    private static final int DOUBLE_CLICK_MS = 500;
    private static final int BLINK_MS = 530;
    private static final int TEXT_COLOR = 0xFFD4D4D4;

    private record ErrorSpan(int line, int colStart, int tokenLen, String message) {}

    private final EditorDocument document = new EditorDocument(this::onTextChanged);
    private final ParameterHints hints = new ParameterHints(this.document::chars);
    private final FindReplaceBar findBar;
    private final SyntaxHighlighter highlighter = new SyntaxHighlighter();
    private List<SyntaxHighlighter.Span> spans = new ArrayList<>();
    private Runnable onChange;

    private int scrollLine = 0;
    private int scrollX = 0;
    private long lastClickTime = 0;
    private int lastClickPos = -1;

    private List<ErrorSpan> errorSpans = new ArrayList<>();
    private String errorMsg = null;
    /** When true, the diagnostics currently shown are warnings (yellow) rather than errors (red).
     *  Errors and warnings are mutually exclusive: warnings are only shown when compilation succeeds. */
    private boolean warnMode = false;
    private int errorScroll = 0;

    private boolean sbDragging = false;
    private int sbDragStartY = 0;
    private int sbDragStartScrollLine = 0;
    private boolean hsbDragging = false;
    private int hsbDragStartX;
    private int hsbDragStartScroll;
    /** Pixel width of the widest line (with its inline hints), or -1 when it must be measured again. */
    private int contentWidthCache = -1;
    // How far right the content reaches at the cursor, hints included; the widest line is picked by
    // characters, so the cursor's line can reach further and must still be scrollable to.
    private int cursorContentWidth = 0;

    /** The open completion list, or null when closed. */
    private EditorSuggestions.Suggestions suggestions = null;
    private int suggestionIndex = 0;
    private int suggestionScrollOffset = 0;
    private int suggPanelX = -1;
    private int suggPanelY = -1;
    private int suggPanelW = 0;
    private int suggPanelH = 0;

    /** The call and argument the cursor is in, whose hint is drawn highlighted. */
    private CallAt activeCall = CallAt.NONE;

    public SyntaxEditorWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
        this.findBar = new FindReplaceBar(this.document, new FindReplaceBar.Host() {
            @Override
            public void revealCursor() {
                ensureCursorVisible();
            }

            @Override
            public String getClipboard() {
                return Minecraft.getInstance().keyboardHandler.getClipboard();
            }

            @Override
            public void setClipboard(String text) {
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
            }
        });
    }

    // ---- public API ----

    public void setOnChange(Runnable r) { this.onChange = r; }

    public String getValue() { return this.document.getText(); }

    public void setValue(String s) {
        this.document.setText(normalizeInput(s == null ? "" : s));
        this.scrollLine = 0;
        this.scrollX = 0;
        this.contentWidthCache = -1;
        this.hints.invalidate();
        rebuildSpans();
    }

    public void setError(int line0, int colStart0, int tokenLen, String msg) {
        this.errorSpans = new ArrayList<>();
        if (line0 >= 0) this.errorSpans.add(new ErrorSpan(line0, colStart0, tokenLen, msg));
        this.errorMsg = msg;
        this.errorScroll = 0;
    }

    public void setErrors(List<int[]> spans, List<String> messages, String firstMsg) {
        this.errorSpans = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            int[] s = spans.get(i);
            String msg = i < messages.size() ? messages.get(i) : firstMsg;
            if (s[0] >= 0) this.errorSpans.add(new ErrorSpan(s[0], s[1], s[2], msg));
        }
        this.errorMsg = firstMsg;
        this.warnMode = false;
        this.errorScroll = 0;
    }

    /** Show compiler warnings (yellow). Reuses the error-bar machinery since warnings and
     *  errors never appear at the same time. */
    public void setWarnings(List<int[]> spans, List<String> messages, String firstMsg) {
        setErrors(spans, messages, firstMsg);
        this.warnMode = true;
    }

    public void clearError() {
        this.errorSpans = new ArrayList<>();
        this.errorMsg = null;
        this.warnMode = false;
        this.errorScroll = 0;
    }

    public void openFind() { this.findBar.openFind(); }

    public void openReplace() { this.findBar.openReplace(); }

    public void closeFindBar() { this.findBar.close(); }

    public boolean isFindOpen() { return this.findBar.isOpen(); }

    public boolean hasSuggestions() {
        return this.suggestions != null;
    }

    public void dismissSuggestions() {
        this.suggestions = null;
        this.suggestionIndex = 0;
        this.suggestionScrollOffset = 0;
        this.suggPanelX = -1;
    }

    // ---- rendering ----

    @Override
    protected void renderContent(Gfx g, int mouseX, int mouseY, float pt) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX();
        int y0 = getY();
        int errorH = errorBarH();
        int codeH = this.height - errorH;
        UITheme t = NeoHouseConfig.get().uiTheme;

        g.fill(x0, y0, x0 + this.width, y0 + codeH, 0xE01E1E1E);
        g.fill(x0, y0, x0 + GUTTER_W, y0 + codeH, 0xE0252526);
        g.fill(x0 + GUTTER_W, y0, x0 + GUTTER_W + 1, y0 + codeH, t.accent());

        List<String> lines = this.document.getLines();
        int visLines = visibleLines();
        this.scrollLine = clampScrollLine(this.scrollLine, lines.size(), visLines);
        this.scrollX = Math.min(this.scrollX, maxScrollX());
        int[] lineOffsets = this.document.lineOffsets();
        int cursorLine = this.document.cursorLineCol()[0];
        int codeAreaRight = x0 + this.width - SB_W - SB_PAD * 2;
        int endLine = Math.min(this.scrollLine + visLines, lines.size());

        g.enableScissor(x0 + GUTTER_W + 1, y0, codeAreaRight, y0 + codeH);
        for (int li = this.scrollLine; li < endLine; li++)
            renderLine(g, font, lines, li, lineOffsets[li], y0 + PAD_Y + (li - this.scrollLine) * LINE_H, li == cursorLine);
        g.disableScissor();

        for (int li = this.scrollLine; li < endLine; li++) {
            int ry = y0 + PAD_Y + (li - this.scrollLine) * LINE_H;
            String num = String.valueOf(li + 1);
            int color = isErrorLine(li) ? (this.warnMode ? 0xFFFFCC66 : 0xFFFF6666) : 0xFF636363;
            g.drawString(font, num, x0 + GUTTER_W - font.width(num) - 3, ry, color, false);
        }

        renderScrollbar(g, x0, y0, codeH, lines.size(), visLines, t);
        renderHorizontalScrollbar(g, codeH, t);
        barMessage().ifPresent(message -> renderErrorBar(g, font, x0, y0 + codeH, errorH, message));

        int border = t.accent();
        g.fill(x0, y0, x0 + this.width, y0 + 1, border);
        g.fill(x0, y0 + this.height - 1, x0 + this.width, y0 + this.height, border);
        g.fill(x0, y0, x0 + 1, y0 + this.height, border);
        g.fill(x0 + this.width - 1, y0, x0 + this.width, y0 + this.height, border);

        this.findBar.render(g, font, t, findBarX(), findBarY());
        renderSuggestions(g, font, x0, y0, lines);
    }

    private void renderLine(Gfx g, Font font, List<String> lines, int li, int lineStart, int ry, boolean isCursorLine) {
        String line = lines.get(li);
        int rowEnd = lineStart + line.length();
        CallAt active = isFocused() && isCursorLine ? this.activeCall : CallAt.NONE;
        List<InlineHint> lineHints = lineHints(lines, li, active);

        if (isErrorLine(li))
            g.fill(getX() + GUTTER_W + 1, ry - 1, getX() + this.width, ry + LINE_H - 1, this.warnMode ? 0x44FFCC33 : 0x44FF3333);

        List<Integer> matches = this.findBar.isOpen() ? this.findBar.getMatches() : List.of();
        int queryLength = this.findBar.queryLength();
        for (int mi = 0; mi < matches.size() && queryLength > 0; mi++) {
            int matchStart = matches.get(mi);
            int matchEnd = matchStart + queryLength;
            if (matchEnd <= lineStart || matchStart >= rowEnd) continue;
            int color = mi == this.findBar.getMatchIndex() ? 0x88FFAA00 : 0x55CCAA00;
            fillColumns(g, font, line, lineHints, Math.max(matchStart, lineStart) - lineStart, Math.min(matchEnd, rowEnd) - lineStart, ry, color);
        }

        if (this.document.hasSelection()) {
            int lo = Math.max(this.document.selectionStart(), lineStart);
            int hi = Math.min(this.document.selectionEnd(), rowEnd);
            if (lo < hi) fillColumns(g, font, line, lineHints, lo - lineStart, hi - lineStart, ry, 0x664477AA);
        }

        renderHighlightedLine(g, font, line, lineStart, codeX() - this.scrollX, ry, lineHints);

        int underline = this.warnMode ? 0xFFFFAA33 : 0xFFFF4444;
        for (ErrorSpan es : this.errorSpans) {
            if (es.line() != li || es.colStart() < 0 || es.tokenLen() <= 0) continue;
            int lo = Math.min(es.colStart(), line.length());
            int hi = Math.min(es.colStart() + es.tokenLen(), line.length());
            if (hi <= lo) continue;
            int ux0 = columnX(font, line, lo, lineHints);
            int ux1 = columnX(font, line, hi, lineHints);
            int uy = ry + LINE_H - 2;
            for (int wx = ux0; wx < ux1; wx++) g.fill(wx, uy + (wx % 2), wx + 1, uy + (wx % 2) + 1, underline);
        }

        if (isFocused() && isCursorLine && (System.currentTimeMillis() / BLINK_MS) % 2 == 0) {
            int cx = columnX(font, line, Math.min(this.document.cursorLineCol()[1], line.length()), lineHints);
            g.fill(cx, ry - 1, cx + 1, ry + LINE_H - 1, 0xFFCCCCCC);
        }
    }

    /** Fills the background of columns {@code [from, to)} of a line. */
    private void fillColumns(Gfx g, Font font, String line, List<InlineHint> lineHints, int from, int to, int ry, int color) {
        g.fill(columnX(font, line, from, lineHints), ry - 1, columnX(font, line, to, lineHints), ry + LINE_H - 1, color);
    }

    /** Screen x of column {@code col} of a line, after scrolling. */
    private int columnX(Font font, String line, int col, List<InlineHint> lineHints) {
        return codeX() + lineColToPixelX(font, line, col, lineHints) - this.scrollX;
    }

    private void renderErrorBar(Gfx g, Font font, int x0, int ey, int errorH, String displayMsg) {
        int barBg = this.warnMode ? 0xEE3B3111 : 0xEE3B1111;
        int barLine = this.warnMode ? 0xFFAAAA33 : 0xFFAA3333;
        int barText = this.warnMode ? 0xFFFFDD88 : 0xFFFF8888;
        g.fill(x0, ey, x0 + this.width, ey + errorH, barBg);
        g.fill(x0, ey, x0 + this.width, ey + 1, barLine);
        List<String> wrapped = wrapErrorMsg(font, this.width - 8, displayMsg);
        int maxScroll = Math.max(0, wrapped.size() - errorBarMaxLines());
        this.errorScroll = Math.max(0, Math.min(this.errorScroll, maxScroll));
        int shownLines = Math.min(wrapped.size(), errorBarMaxLines());
        for (int i = 0; i < shownLines && this.errorScroll + i < wrapped.size(); i++)
            g.drawString(font, wrapped.get(this.errorScroll + i), x0 + 4, ey + 3 + i * 10, barText, false);
        if (this.errorScroll > 0) g.drawString(font, "▲", x0 + this.width - 10, ey + 3, 0xFFFFCC66, false);
        if (this.errorScroll < maxScroll) g.drawString(font, "▼", x0 + this.width - 10, ey + errorH - 11, 0xFFFFCC66, false);
    }

    private void renderScrollbar(Gfx g, int x0, int y0, int codeH, int totalLines, int visLines, UITheme t) {
        if (totalLines <= visLines) return;
        int trackX = x0 + this.width - SB_W - SB_PAD;
        g.fill(trackX, y0, trackX + SB_W, y0 + codeH, 0x33FFFFFF);
        int maxOff = totalLines - visLines;
        int thumbH = Math.max(8, codeH * visLines / totalLines);
        int thumbY = y0 + (codeH - thumbH) * this.scrollLine / maxOff;
        int color = this.sbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, color);
    }

    private void renderHorizontalScrollbar(Gfx g, int codeH, UITheme t) {
        int maxScroll = maxScrollX();
        if (maxScroll <= 0) return;
        int[] track = hsbTrack(codeH);
        int thumbW = hsbThumbWidth(track[2]);
        int thumbX = track[0] + (track[2] - thumbW) * this.scrollX / maxScroll;
        g.fill(track[0], track[1], track[0] + track[2], track[1] + HSB_H, 0x33FFFFFF);
        int color = this.hsbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(thumbX, track[1], thumbX + thumbW, track[1] + HSB_H, color);
    }

    private void renderHighlightedLine(Gfx g, Font font, String line, int lineStart, int x, int y, List<InlineHint> lineHints) {
        int lineEnd = lineStart + line.length();
        int curX = x;
        int charDrawn = 0;
        int hintPtr = 0;

        // Spans are sorted and non-overlapping, so jump straight to the first one touching this
        // line instead of scanning from the start (which made deep scrolling O(spans) per line).
        for (int si = firstSpanTouching(lineStart); si < this.spans.size(); si++) {
            SyntaxHighlighter.Span sp = this.spans.get(si);
            if (sp.start() >= lineEnd) break;

            int lo = Math.max(0, Math.min(sp.start() - lineStart, line.length()));
            int hi = Math.max(lo, Math.min(sp.end() - lineStart, line.length()));
            lo = Math.max(lo, charDrawn);

            if (lo > charDrawn) {
                int[] r = drawSegment(g, font, line, charDrawn, lo, TEXT_COLOR, 0, lineHints, hintPtr, curX, y);
                curX = r[0]; charDrawn = r[1]; hintPtr = r[2];
            }
            if (hi > charDrawn) {
                int spanColor = sp.color() == 0 ? TEXT_COLOR : sp.color();
                int[] r = drawSegment(g, font, line, charDrawn, hi, spanColor, sp.style(), lineHints, hintPtr, curX, y);
                curX = r[0]; charDrawn = r[1]; hintPtr = r[2];
            }
        }
        // to = length + 1 so a hint at the very end of the line (an empty argument being typed) is drawn too.
        drawSegment(g, font, line, charDrawn, line.length() + 1, TEXT_COLOR, 0, lineHints, hintPtr, curX, y);
    }

    /** Draws columns {@code [from, to)} with the hints inside them; returns {x after, to, next hint index}. */
    private int[] drawSegment(Gfx g, Font font, String line, int from, int to, int color, int style,
                              List<InlineHint> lineHints, int hintPtr, int curX, int y) {
        int pos = from;
        UITheme theme = NeoHouseConfig.get().uiTheme;
        while (hintPtr < lineHints.size()) {
            InlineHint hint = lineHints.get(hintPtr);
            int hOff = hint.charOffset();
            if (hOff >= to) break;
            if (hOff < from) { hintPtr++; continue; }

            if (hOff > pos) {
                String seg = line.substring(pos, Math.min(hOff, line.length()));
                curX += StyledText.draw(g, font, seg, curX, y, color, style);
                pos = hOff;
            }
            String ht = hint.displayText();
            int htColor = hint.isActive() ? theme.muted() : theme.divider();
            g.drawString(font, Component.literal(ht).withStyle(ChatFormatting.ITALIC), curX, y, htColor, false);
            curX += font.width(ht);
            hintPtr++;
        }
        int drawEnd = Math.min(to, line.length());
        if (drawEnd > pos) {
            String seg = line.substring(pos, drawEnd);
            curX += StyledText.draw(g, font, seg, curX, y, color, style);
        }
        return new int[]{curX, to, hintPtr};
    }

    private void renderSuggestions(Gfx g, Font font, int x0, int y0, List<String> lines) {
        if (this.suggestions == null || !isFocused()) {
            this.suggPanelX = -1;
            return;
        }
        int[] curLC = this.document.cursorLineCol();
        if (curLC[0] < this.scrollLine || curLC[0] >= this.scrollLine + visibleLines()) {
            this.suggPanelX = -1;
            return;
        }
        UITheme t = NeoHouseConfig.get().uiTheme;
        List<String> items = this.suggestions.items();
        String curLine = lines.get(curLC[0]);
        int cx = columnX(font, curLine, Math.min(curLC[1], curLine.length()), lineHints(lines, curLC[0], CallAt.NONE));
        int cy = y0 + PAD_Y + (curLC[0] - this.scrollLine) * LINE_H + LINE_H;

        int start = this.suggestionScrollOffset;
        int end = Math.min(items.size(), start + MAX_VISIBLE_SUGGESTIONS);
        int panelW = 40;
        for (String s : items) panelW = Math.max(panelW, font.width(s) + 10);
        int panelH = (end - start) * LINE_H + 4;

        int codeBottom = y0 + this.height - errorBarH();
        int px = Math.max(x0 + GUTTER_W + 1, Math.min(cx, x0 + this.width - panelW - 1));
        int py = cy + panelH > codeBottom ? Math.max(y0, cy - panelH - LINE_H) : cy;
        this.suggPanelX = px;
        this.suggPanelY = py;
        this.suggPanelW = panelW;
        this.suggPanelH = panelH;

        g.fill(px, py, px + panelW, py + panelH, t.dialog(0xF0));
        g.fill(px, py, px + panelW, py + 1, t.accent());
        g.fill(px, py + panelH - 1, px + panelW, py + panelH, t.panel(0xFF));
        g.fill(px, py, px + 1, py + panelH, t.accent());
        g.fill(px + panelW - 1, py, px + panelW, py + panelH, t.panel(0xFF));

        for (int si = start; si < end; si++) {
            int sy = py + 2 + (si - start) * LINE_H;
            if (si == this.suggestionIndex) g.fill(px + 1, sy - 1, px + panelW - 1, sy + LINE_H - 1, t.accentTint(0x55));
            g.drawString(font, items.get(si), px + 4, sy, TEXT_COLOR, false);
        }
        if (items.size() > MAX_VISIBLE_SUGGESTIONS) {
            if (start > 0) g.drawString(font, "▲", px + panelW - 9, py + 2, 0xFF888888, false);
            if (end < items.size()) g.drawString(font, "▼", px + panelW - 9, py + panelH - LINE_H, 0xFF888888, false);
        }
    }

    // ---- input ----

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & Keys.MOD_CONTROL) != 0;
        boolean shift = (mods & Keys.MOD_SHIFT) != 0;
        boolean alt = (mods & Keys.MOD_ALT) != 0;

        if (this.findBar.hasFocus() && this.findBar.keyPressed(key, mods)) return true;

        if (ctrl && key == Keys.F) { openFind(); return true; }
        if (ctrl && key == Keys.R) { openReplace(); return true; }

        if (!ctrl && !shift && !alt && this.suggestions != null) {
            int count = this.suggestions.items().size();
            switch (key) {
                case Keys.TAB -> { applySuggestion(); return true; }
                case Keys.UP -> { this.suggestionIndex = Math.floorMod(this.suggestionIndex - 1, count); clampSuggestionScroll(); return true; }
                case Keys.DOWN -> { this.suggestionIndex = (this.suggestionIndex + 1) % count; clampSuggestionScroll(); return true; }
                case Keys.ESCAPE -> { dismissSuggestions(); return true; }
                default -> { }
            }
        }

        if (ctrl) {
            switch (key) {
                case Keys.A -> { this.document.selectAll(); return true; }
                case Keys.C -> { copySelection(); return true; }
                case Keys.X -> { cutSelection(); return true; }
                case Keys.V -> { pasteClipboard(); return true; }
                case Keys.Z -> { if (shift) redo(); else undo(); return true; }
                case Keys.Y -> { redo(); return true; }
                case Keys.HOME -> { moveCursor(0, shift); return true; }
                case Keys.END -> { moveCursor(this.document.length(), shift); return true; }
                case Keys.LEFT -> { moveCursor(this.document.wordLeftOf(this.document.getCursor()), shift); return true; }
                case Keys.RIGHT -> { moveCursor(this.document.wordRightOf(this.document.getCursor()), shift); return true; }
                case Keys.BACKSPACE -> { this.document.deleteWordBackward(); return true; }
                case Keys.SLASH -> { this.document.toggleLineComments(); ensureCursorVisible(); return true; }
                case Keys.D -> { this.document.duplicateLines(); ensureCursorVisible(); return true; }
                default -> { }
            }
        }

        if (alt && (key == Keys.UP || key == Keys.DOWN)) {
            this.document.moveLines(key == Keys.UP ? -1 : 1);
            ensureCursorVisible();
            return true;
        }

        switch (key) {
            case Keys.TAB -> {
                if (this.document.hasSelection()) {
                    if (shift) this.document.dedentSelectedLines(spacesPerTab());
                    else this.document.indentSelectedLines(spacesPerTab());
                } else {
                    if (shift) this.document.dedentLine(spacesPerTab());
                    else this.document.insert(" ".repeat(spacesPerTab()));
                }
                return true;
            }
            case Keys.BACKSPACE -> { this.document.deleteBackward(); return true; }
            case Keys.DELETE -> { this.document.deleteForward(); return true; }
            case Keys.ENTER, Keys.KP_ENTER -> { this.document.insertNewline(NeoHouseConfig.get().autoIndent); return true; }
            case Keys.LEFT -> { moveHorizontal(-1, shift); return true; }
            case Keys.RIGHT -> { moveHorizontal(1, shift); return true; }
            case Keys.UP -> { moveVertical(-1, shift); return true; }
            case Keys.DOWN -> { moveVertical(1, shift); return true; }
            case Keys.HOME -> { moveToLineEdge(true, shift); return true; }
            case Keys.END -> { moveToLineEdge(false, shift); return true; }
            case Keys.PAGE_UP -> { moveVertical(-visibleLines(), shift); return true; }
            case Keys.PAGE_DOWN -> { moveVertical(visibleLines(), shift); return true; }
            case Keys.F12 -> { goToDefinition(this.document.getCursor()); return true; }
            default -> { return false; }
        }
    }

    @Override
    protected boolean onCharTyped(char c, int mods) {
        if (c < 32 || c == 127) return false;
        if (this.findBar.hasFocus()) return this.findBar.charTyped(c);
        this.document.insert(String.valueOf(c));
        return true;
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) {
            setFocused(false);
            this.suggestions = null;
            return false;
        }
        Font font = Minecraft.getInstance().font;
        if (btn == 0 && this.findBar.mouseClicked(font, mx, my, findBarX(), findBarY())) return true;

        int codeH = this.height - errorBarH();
        if (btn == 0 && clickHorizontalScrollbar(mx, my, codeH)) return true;
        if (clickVerticalScrollbar(mx, my, codeH)) return true;
        if (clickSuggestionPanel(mx, my)) return true;

        setFocused(true);
        this.findBar.focusCode();
        int pos = hitToOffset((int) mx, (int) my);
        long now = System.currentTimeMillis();
        boolean isDoubleClick = now - this.lastClickTime < DOUBLE_CLICK_MS && pos == this.lastClickPos;
        this.lastClickTime = now;
        this.lastClickPos = pos;
        if (RenderHelper.hasControlDown() && goToDefinition(pos)) {
            dismissSuggestions();
            return true;
        }
        if (RenderHelper.hasShiftDown()) this.document.moveCursor(pos, true); // extends the selection from the anchor
        else if (isDoubleClick) this.document.selectWordAt(pos);
        else this.document.moveCursor(pos, false);
        dismissSuggestions();
        updateActiveCall();
        return true;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (this.hsbDragging && btn == 0) {
            int[] track = hsbTrack(this.height - errorBarH());
            int slideW = track[2] - hsbThumbWidth(track[2]);
            if (slideW > 0)
                this.scrollX = Math.max(0, Math.min(maxScrollX(), this.hsbDragStartScroll + (int) (mx - this.hsbDragStartX) * maxScrollX() / slideW));
            return true;
        }
        if (this.sbDragging && btn == 0) {
            int codeH = this.height - errorBarH();
            int lineCount = this.document.getLines().size();
            int visLines = visibleLines();
            int thumbH = Math.max(8, codeH * visLines / lineCount);
            int maxOff = Math.max(0, lineCount - visLines);
            int slideH = codeH - thumbH;
            if (slideH > 0)
                this.scrollLine = Math.max(0, Math.min(maxOff, this.sbDragStartScrollLine + (int) (my - this.sbDragStartY) * maxOff / slideH));
            return true;
        }

        if (btn != 0 || !isFocused()) return false;
        this.document.moveCursor(hitToOffset((int) mx, (int) my), true);
        ensureCursorVisible();
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int btn) {
        if (this.hsbDragging && btn == 0) { this.hsbDragging = false; return true; }
        if (this.sbDragging && btn == 0) { this.sbDragging = false; return true; }
        return superMouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!isMouseOver(mx, my)) return false;

        if (barMessage().isPresent() && my >= getY() + this.height - errorBarH() && my < getY() + this.height) {
            this.errorScroll -= (int) Math.signum(dy);
            return true;
        }
        if (this.suggestions != null && this.suggestions.items().size() > MAX_VISIBLE_SUGGESTIONS && insideSuggestionPanel(mx, my)) {
            int maxOff = this.suggestions.items().size() - MAX_VISIBLE_SUGGESTIONS;
            this.suggestionScrollOffset = Math.max(0, Math.min(maxOff, this.suggestionScrollOffset - (int) Math.signum(dy)));
            return true;
        }
        if (RenderHelper.hasShiftDown() || dx != 0) {
            double amount = dx != 0 ? dx : dy;
            this.scrollX = Math.max(0, Math.min(maxScrollX(), this.scrollX - (int) Math.signum(amount) * H_SCROLL_STEP));
            return true;
        }
        this.scrollLine = clampScrollLine(this.scrollLine - (int) Math.signum(dy), this.document.getLines().size(), visibleLines());
        return true;
    }

    private boolean clickHorizontalScrollbar(double mx, double my, int codeH) {
        int maxScroll = maxScrollX();
        if (maxScroll <= 0) return false;
        int[] track = hsbTrack(codeH);
        if (mx < track[0] || mx >= track[0] + track[2] || my < track[1] - 2 || my > track[1] + HSB_H + 2) return false;
        int thumbW = hsbThumbWidth(track[2]);
        int thumbX = track[0] + (track[2] - thumbW) * this.scrollX / maxScroll;
        if (mx < thumbX || mx >= thumbX + thumbW) {
            int slideW = Math.max(1, track[2] - thumbW);
            this.scrollX = Math.max(0, Math.min(maxScroll, (int) ((mx - track[0] - thumbW / 2.0) * maxScroll / slideW)));
        }
        this.hsbDragging = true;
        this.hsbDragStartX = (int) mx;
        this.hsbDragStartScroll = this.scrollX;
        return true;
    }

    private boolean clickVerticalScrollbar(double mx, double my, int codeH) {
        int lineCount = this.document.getLines().size();
        int visLines = visibleLines();
        if (lineCount <= visLines) return false;
        int y0 = getY();
        int trackX = getX() + this.width - SB_W - SB_PAD;
        if (mx < trackX - 2 || mx > trackX + SB_W + 2 || my < y0 || my >= y0 + codeH) return false;
        int maxOff = lineCount - visLines;
        int thumbH = Math.max(8, codeH * visLines / lineCount);
        int thumbY = y0 + (codeH - thumbH) * this.scrollLine / maxOff;
        if (my >= thumbY && my < thumbY + thumbH) {
            this.sbDragging = true;
            this.sbDragStartY = (int) my;
            this.sbDragStartScrollLine = this.scrollLine;
        } else {
            int newScroll = (int) ((my - y0 - thumbH / 2.0) * maxOff / Math.max(1, codeH - thumbH));
            this.scrollLine = Math.max(0, Math.min(maxOff, newScroll));
        }
        return true;
    }

    /** A click in the suggestion list selects an entry; a double click applies it. */
    private boolean clickSuggestionPanel(double mx, double my) {
        if (this.suggestions == null || !insideSuggestionPanel(mx, my)) return false;
        int si = ((int) my - this.suggPanelY - 2) / LINE_H + this.suggestionScrollOffset;
        if (si >= 0 && si < this.suggestions.items().size()) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = now - this.lastClickTime < DOUBLE_CLICK_MS && si == this.suggestionIndex;
            this.lastClickTime = now;
            this.lastClickPos = -1;
            if (isDoubleClick) applySuggestion();
            else this.suggestionIndex = si;
        }
        return true;
    }

    private boolean insideSuggestionPanel(double mx, double my) {
        return this.suggPanelX >= 0 && mx >= this.suggPanelX && mx < this.suggPanelX + this.suggPanelW
                && my >= this.suggPanelY && my < this.suggPanelY + this.suggPanelH;
    }

    // ---- cursor movement and clipboard ----

    private void moveCursor(int pos, boolean select) {
        this.document.moveCursor(pos, select);
        ensureCursorVisible();
        refreshOpenSuggestions();
    }

    private void moveHorizontal(int direction, boolean select) {
        if (!select && this.document.hasSelection()) {
            this.document.collapseSelection(direction < 0);
            ensureCursorVisible();
            refreshOpenSuggestions();
            return;
        }
        moveCursor(this.document.getCursor() + direction, select);
    }

    private void moveVertical(int lineDelta, boolean select) {
        List<String> lines = this.document.getLines();
        int[] lc = this.document.cursorLineCol();
        int target = Math.max(0, Math.min(lc[0] + lineDelta, lines.size() - 1));
        moveCursor(this.document.lineOffsets()[target] + Math.min(lc[1], lines.get(target).length()), select);
    }

    private void moveToLineEdge(boolean home, boolean select) {
        int line = this.document.cursorLineCol()[0];
        int start = this.document.lineOffsets()[line];
        moveCursor(home ? start : start + this.document.getLines().get(line).length(), select);
    }

    /** Ctrl+click / F12: selects the declaration of the function named at {@code pos}; false when there is none. */
    private boolean goToDefinition(int pos) {
        Optional<int[]> word = this.document.identifierAt(pos);
        if (word.isEmpty()) return false;
        String name = this.document.chars().subSequence(word.get()[0], word.get()[1]).toString();
        Optional<Integer> definition = ParameterHints.functionDefinition(this.document.chars(), name);
        if (definition.isEmpty()) return false;
        this.document.select(definition.get(), definition.get() + name.length());
        ensureCursorVisible();
        updateActiveCall();
        return true;
    }

    private void copySelection() {
        if (this.document.hasSelection()) Minecraft.getInstance().keyboardHandler.setClipboard(this.document.selectedText());
    }

    private void cutSelection() {
        String removed = this.document.cutSelectionOrLine();
        if (!removed.isEmpty()) Minecraft.getInstance().keyboardHandler.setClipboard(removed);
    }

    private void pasteClipboard() {
        this.document.insert(normalizeInput(Minecraft.getInstance().keyboardHandler.getClipboard()));
    }

    private void undo() {
        if (this.document.undo()) ensureCursorVisible();
    }

    private void redo() {
        if (this.document.redo()) ensureCursorVisible();
    }

    /** Line endings to '\n' and tabs to spaces, as the editor stores text. */
    private String normalizeInput(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n").replace("\t", " ".repeat(spacesPerTab()));
    }

    // ---- reacting to edits ----

    private void onTextChanged() {
        this.contentWidthCache = -1;
        this.hints.invalidate();
        rebuildSpans();
        this.findBar.onTextChanged();
        updateSuggestions();
        // Every edit shows where it happened, also when typing past the right edge.
        ensureCursorVisible();
        if (this.onChange != null) this.onChange.run();
    }

    private void rebuildSpans() {
        this.spans = this.highlighter.buildSpans(this.document.getText());
    }

    /** Index of the first span whose end is past {@code lineStart} (spans are sorted by end). */
    private int firstSpanTouching(int lineStart) {
        int lo = 0;
        int hi = this.spans.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (this.spans.get(mid).end() <= lineStart) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Recomputes the suggestion list while it's open (closing it if nothing matches); never opens it. */
    private void refreshOpenSuggestions() {
        if (this.suggestions != null) updateSuggestions();
        else updateActiveCall();
    }

    private void updateSuggestions() {
        EditorSuggestions.Suggestions previous = this.suggestions;
        this.suggestions = NeoHouseConfig.get().showArgumentRecommendations
                ? EditorSuggestions.at(textBeforeCursor()).orElse(null)
                : null;
        if (this.suggestions == null || previous == null || previous.items().size() != this.suggestions.items().size()) {
            this.suggestionIndex = 0;
            this.suggestionScrollOffset = 0;
        } else {
            this.suggestionIndex = Math.min(this.suggestionIndex, this.suggestions.items().size() - 1);
        }
        updateActiveCall();
    }

    private void applySuggestion() {
        if (this.suggestions == null) return;
        List<String> items = this.suggestions.items();
        String suggestion = items.get(Math.min(this.suggestionIndex, items.size() - 1));
        int cursor = this.document.getCursor();
        int from = cursor - this.suggestions.typedLength();
        boolean needsClosingQuote = this.suggestions.insideString()
                && (cursor >= this.document.length() || this.document.chars().charAt(cursor) != '"');
        this.document.replace(from, cursor, needsClosingQuote ? suggestion + "\"" : suggestion);
    }

    private void clampSuggestionScroll() {
        if (this.suggestions == null) return;
        int maxOff = Math.max(0, this.suggestions.items().size() - MAX_VISIBLE_SUGGESTIONS);
        if (this.suggestionIndex < this.suggestionScrollOffset)
            this.suggestionScrollOffset = this.suggestionIndex;
        else if (this.suggestionIndex >= this.suggestionScrollOffset + MAX_VISIBLE_SUGGESTIONS)
            this.suggestionScrollOffset = this.suggestionIndex - MAX_VISIBLE_SUGGESTIONS + 1;
        this.suggestionScrollOffset = Math.max(0, Math.min(maxOff, this.suggestionScrollOffset));
    }

    /** Finds which call and argument the cursor is in, so that argument's hint is highlighted. */
    private void updateActiveCall() {
        this.activeCall = CallAt.NONE;
        Optional<CallAt> call = ParameterHints.callAt(textBeforeCursor());
        if (call.isEmpty()) return;
        List<String> labels = this.hints.parameterLabels(call.get().functionName());
        if (labels.isEmpty()) return;
        this.activeCall = new CallAt(call.get().functionName(), Math.min(call.get().argIndex(), labels.size() - 1));
    }

    private String textBeforeCursor() {
        int[] lc = this.document.cursorLineCol();
        return this.document.getLines().get(lc[0]).substring(0, lc[1]);
    }

    // ---- layout and coordinates ----

    private List<InlineHint> lineHints(List<String> lines, int lineIndex, CallAt active) {
        if (!NeoHouseConfig.get().showArgumentHints) return List.of();
        return this.hints.forLine(lines, lineIndex, cursorColumnOn(lineIndex), active);
    }

    /** The text cursor's column when it is on line {@code lineIndex} of the focused editor, else -1. */
    private int cursorColumnOn(int lineIndex) {
        if (!isFocused()) return -1;
        int[] lc = this.document.cursorLineCol();
        return lc[0] == lineIndex ? lc[1] : -1;
    }

    private void ensureCursorVisible() {
        List<String> lines = this.document.getLines();
        int[] lc = this.document.cursorLineCol();
        int vis = visibleLines();
        if (lc[0] < this.scrollLine) this.scrollLine = lc[0];
        if (lc[0] >= this.scrollLine + vis) this.scrollLine = lc[0] - vis + 1;
        this.scrollLine = clampScrollLine(this.scrollLine, lines.size(), vis);

        Font font = Minecraft.getInstance().font;
        String line = lines.get(lc[0]);
        int cursorPx = lineColToPixelX(font, line, Math.min(lc[1], line.length()), lineHints(lines, lc[0], CallAt.NONE));
        int viewW = codeViewWidth();
        this.cursorContentWidth = cursorPx + PAD_X;
        if (cursorPx < this.scrollX) this.scrollX = cursorPx;
        if (cursorPx > this.scrollX + viewW - 4) this.scrollX = cursorPx - viewW + 4;
        this.scrollX = Math.max(0, Math.min(this.scrollX, maxScrollX()));
    }

    private int hitToOffset(int mx, int my) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = this.document.getLines();
        int li = Math.max(0, Math.min(this.scrollLine + (my - (getY() + PAD_Y)) / LINE_H, lines.size() - 1));
        String line = lines.get(li);
        int col = pixelXToLineCol(font, line, mx - codeX() + this.scrollX, lineHints(lines, li, CallAt.NONE));
        return this.document.lineOffsets()[li] + col;
    }

    private static int lineColToPixelX(Font font, String line, int col, List<InlineHint> lineHints) {
        int charPos = 0;
        int pixW = 0;
        for (InlineHint h : lineHints) {
            if (h.charOffset() >= col) break;
            int segEnd = Math.min(h.charOffset(), line.length());
            if (segEnd > charPos) {
                pixW += font.width(line.substring(charPos, segEnd));
                charPos = segEnd;
            }
            pixW += font.width(h.displayText());
        }
        int segEnd = Math.min(col, line.length());
        if (segEnd > charPos) pixW += font.width(line.substring(charPos, segEnd));
        return pixW;
    }

    private static int pixelXToLineCol(Font font, String line, int relX, List<InlineHint> lineHints) {
        int charPos = 0;
        int pixConsumed = 0;
        for (InlineHint h : lineHints) {
            while (charPos < Math.min(h.charOffset(), line.length())) {
                int cw = font.width(String.valueOf(line.charAt(charPos)));
                if (pixConsumed + (cw + 1) / 2 >= relX) return charPos;
                pixConsumed += cw;
                charPos++;
            }
            if (charPos < h.charOffset()) break;
            int hw = font.width(h.displayText());
            if (pixConsumed + hw > relX) return h.charOffset();
            pixConsumed += hw;
        }
        while (charPos < line.length()) {
            int cw = font.width(String.valueOf(line.charAt(charPos)));
            if (pixConsumed + (cw + 1) / 2 >= relX) return charPos;
            pixConsumed += cw;
            charPos++;
        }
        return line.length();
    }

    private int findBarX() { return getX() + this.width - FindReplaceBar.WIDTH - 2; }

    private int findBarY() { return getY() + 2; }

    private int codeX() { return getX() + GUTTER_W + 1 + PAD_X; }

    /** Width available for code text between the gutter and the vertical scrollbar. */
    private int codeViewWidth() {
        return Math.max(20, this.width - GUTTER_W - 1 - PAD_X * 2 - SB_W - SB_PAD * 2);
    }

    private int contentWidth() {
        if (this.contentWidthCache >= 0) return this.contentWidthCache;
        List<String> lines = this.document.getLines();
        int widest = 0;
        for (int i = 1; i < lines.size(); i++) if (lines.get(i).length() > lines.get(widest).length()) widest = i;
        String line = lines.get(widest);
        Font font = Minecraft.getInstance().font;
        this.contentWidthCache = lineColToPixelX(font, line, line.length(), lineHints(lines, widest, CallAt.NONE)) + PAD_X;
        return this.contentWidthCache;
    }

    private int scrollableWidth() {
        return Math.max(contentWidth(), this.cursorContentWidth);
    }

    private int maxScrollX() {
        return Math.max(0, scrollableWidth() - codeViewWidth());
    }

    /** {x, y, width} of the horizontal scrollbar track along the bottom of the code area. */
    private int[] hsbTrack(int codeH) {
        int left = getX() + GUTTER_W + 1;
        int right = getX() + this.width - SB_W - SB_PAD * 2;
        return new int[]{left, getY() + codeH - HSB_H - 1, right - left};
    }

    private int hsbThumbWidth(int trackW) {
        int viewW = codeViewWidth();
        return Math.max(HSB_MIN_THUMB_W, trackW * viewW / Math.max(viewW, scrollableWidth()));
    }

    private static int clampScrollLine(int s, int lineCount, int vis) {
        return Math.max(0, Math.min(s, Math.max(0, lineCount - vis)));
    }

    /** Lines that fit in the code area, leaving room for the horizontal scrollbar when it shows. */
    private int visibleLines() {
        int codeH = this.height - errorBarH() - (maxScrollX() > 0 ? HSB_H + 1 : 0);
        return Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
    }

    // ---- errors ----

    private boolean isErrorLine(int li) {
        for (ErrorSpan es : this.errorSpans) if (es.line() == li) return true;
        return false;
    }

    /** The message of the error under the selection, else of one on the cursor's line. */
    private Optional<String> getActiveErrorMsg() {
        if (this.errorSpans.isEmpty()) return Optional.empty();
        int selLo = this.document.selectionStart();
        int selHi = this.document.selectionEnd();
        List<String> lines = this.document.getLines();
        int[] offs = this.document.lineOffsets();
        for (ErrorSpan es : this.errorSpans) {
            if (es.line() < 0 || es.line() >= lines.size()) continue;
            int spanStart = offs[es.line()] + es.colStart();
            int spanEnd = spanStart + Math.max(1, es.tokenLen());
            if (selLo < spanEnd && selHi >= spanStart) return Optional.of(es.message());
        }
        int line = this.document.lineColOf(Math.max(selLo, this.document.getCursor() - 1))[0];
        for (ErrorSpan es : this.errorSpans) if (es.line() == line) return Optional.of(es.message());
        return Optional.empty();
    }

    /**
     * The message the bar at the bottom shows: an error always (the one under the cursor, else the
     * first), a warning only while the cursor or selection is on it, so warnings don't take room.
     */
    private Optional<String> barMessage() {
        if (this.errorMsg == null) return Optional.empty();
        Optional<String> active = getActiveErrorMsg();
        return this.warnMode ? active : Optional.of(active.orElse(this.errorMsg));
    }

    private int errorBarH() {
        Optional<String> message = barMessage();
        if (message.isEmpty()) return 0;
        Font font = Minecraft.getInstance().font;
        int shownLines = Math.min(wrapErrorMsg(font, this.width - 8, message.get()).size(), errorBarMaxLines());
        return Math.max(ERROR_BAR_H_MIN, shownLines * 10 + 4);
    }

    private static List<String> wrapErrorMsg(Font font, int maxW, String msg) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : firstMeaningfulLine(msg).split(" ", -1)) {
            if (current.isEmpty()) {
                current.append(word);
            } else if (font.width(current + " " + word) > maxW) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current.append(' ').append(word);
            }
        }
        if (!current.isEmpty()) result.add(current.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }

    private static String firstMeaningfulLine(String s) {
        for (String part : s.split("\n")) {
            String t = part.trim();
            if (!t.isEmpty()) return t;
        }
        return s.trim();
    }

    private int errorBarMaxLines() {
        return Math.max(1, Math.min(20, NeoHouseConfig.get().errorBarMaxLines));
    }

    private int spacesPerTab() {
        return Math.max(1, Math.min(16, NeoHouseConfig.get().tabWidth));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {}
}
