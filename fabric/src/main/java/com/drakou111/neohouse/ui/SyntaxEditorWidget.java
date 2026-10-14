package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.compat.Gfx;
import com.drakou111.neohouse.compat.Keys;
import com.drakou111.neohouse.compat.RenderHelper;
import com.drakou111.neohouse.compat.WidgetBase;
import com.drakou111.neohouse.compiler.enums.ActionParamKind;
import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.UITheme;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import com.drakou111.neohouse.constants.Teleport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxEditorWidget extends WidgetBase {

    private final StringBuilder text = new StringBuilder();
    private int cursor    = 0;
    private int selAnchor = 0;
    private int scrollLine = 0;
    private int scrollX    = 0;

    private long lastClickTime = 0;
    private int  lastClickPos  = -1;

    private List<String> lineCache   = null;
    private int[]        offsetCache = null;

    private record ErrorSpan(int line, int colStart, int tokenLen, String message) {}
    private List<ErrorSpan> errorSpans = new ArrayList<>();
    private String errorMsg = null;
    /** When true, the diagnostics currently shown are warnings (yellow) rather than errors (red).
     *  Errors and warnings are mutually exclusive: warnings are only shown when compilation succeeds. */
    private boolean warnMode = false;

    private Runnable onChange;

    private static final int LINE_H = 10;
    private static final int GUTTER_W = 30;
    private static final int PAD_X  = 4;
    private static final int PAD_Y  = 2;
    private static final int ERROR_BAR_H_MIN = 14;
    private int errorScroll = 0;

    private int errorBarMaxLines() {
        return Math.max(1, Math.min(20, NeoHouseConfig.get().errorBarMaxLines));
    }

    private int spacesPerTab() {
        return Math.max(1, Math.min(16, NeoHouseConfig.get().tabWidth));
    }
    private static final int SB_W = 4;
    private static final int SB_PAD  = 2;

    private boolean sbDragging = false;
    private int sbDragStartY = 0;
    private int sbDragStartScrollLine = 0;

    private static final class FindReplaceState {
        int mode = 0;
        final StringBuilder query   = new StringBuilder();
        final StringBuilder replace = new StringBuilder();
        int cursor = 0;
        int selAnchor = 0;
        int replaceCursor = 0;
        int replaceSelAnchor = 0;
        /** 0 = find field, 1 = replace field, {@link #FOCUS_CODE} = the code (the bar stays open). */
        int focused = 0;
        int matchIndex = -1;
        final List<Integer> matches = new ArrayList<>();
        int btnReplaceX = -1, btnReplaceY = -1;
        int btnReplaceAllX = -1, btnReplaceAllY = -1;
    }
    private final FindReplaceState fr = new FindReplaceState();
    private static final int FOCUS_CODE = -1;
    private static final int FIND_BAR_W = 260;
    private static final int FIND_FIELD_H = 12;
    private static final int FIND_BTN_W = 60;

    private final SyntaxHighlighter highlighter = new SyntaxHighlighter();
    private List<SyntaxHighlighter.Span> spans = new ArrayList<>();

    private record InlineHint(int charOffset, String displayText, boolean isActive) {}

    private final EditorUndoManager undoManager = new EditorUndoManager();

    private String[] activeSuggestions = null;
    private int suggestionIndex = 0;
    private int suggestionScrollOffset = 0;
    private int suggestionTypedLen = 0;
    private boolean  suggestionIsString = false;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    private static final int CONTINUATION_SCAN_LINES = 30;
    private static final int HSB_H = 4;
    private static final int HSB_MIN_THUMB_W = 12;
    private static final int H_SCROLL_STEP = 20;

    /** Pixel width of the widest line (with its inline hints), or -1 when it must be measured again. */
    private int contentWidthCache = -1;
    private boolean hsbDragging = false;
    private int hsbDragStartX, hsbDragStartScroll;
    private int suggPanelX = -1, suggPanelY = -1, suggPanelW = 0, suggPanelH = 0;

    private int    paramHintIndex = -1;
    private String paramHintFunc  = null;
    private static final Pattern USER_FUNC_PAT =
            Pattern.compile("\\bfunction\\b\\s+(?:\\w+\\s+)?(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern PH_SUGGEST_PAT = Pattern.compile("ph\\.(\\w*)$");
    private static final Pattern TRAILING_WORD_PAT = Pattern.compile("([A-Za-z_]*)$");

    private static final Map<String, String[]> PH_PARAM_HINTS = Map.ofEntries(
        Map.entry("playerTeamPlayers",   new String[]{"teamName: string"}),
        Map.entry("dateDayTimezone",     new String[]{"timezone: string"}),
        Map.entry("dateMonthTimezone",   new String[]{"timezone: string"}),
        Map.entry("dateYearTimezone",    new String[]{"timezone: string"}),
        Map.entry("dateHourTimezone",    new String[]{"timezone: string"}),
        Map.entry("dateMinuteTimezone",  new String[]{"timezone: string"}),
        Map.entry("dateSecondsTimezone", new String[]{"timezone: string"}),
        Map.entry("randomWholeCapped",   new String[]{"max: long"}),
        Map.entry("randomWholeRanged",   new String[]{"min: long", "max: long"}),
        Map.entry("randomDecimalCapped", new String[]{"max: double"}),
        Map.entry("randomDecimalRanged", new String[]{"min: double", "max: double"})
    );

    public SyntaxEditorWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Component.empty());
    }

    public void setOnChange(Runnable r) { this.onChange = r; }

    public String getValue() { return text.toString(); }

    public void setValue(String s) {
        text.setLength(0);
        String normalized = (s == null ? "" : s)
                .replace("\r\n", "\n").replace("\r", "\n")
                .replace("\t", " ".repeat(spacesPerTab()));
        text.append(normalized);
        cursor = 0;
        selAnchor = 0;
        scrollLine = 0;
        scrollX = 0;
        contentWidthCache = -1;
        lineCache = null;
        offsetCache = null;
        rebuildSpans();
    }

    public void setError(int line0, int colStart0, int tokenLen, String msg) {
        this.errorSpans = new ArrayList<>();
        if (line0 >= 0) errorSpans.add(new ErrorSpan(line0, colStart0, tokenLen, msg));
        this.errorMsg = msg;
        this.errorScroll = 0;
    }

    public void setErrors(List<int[]> spans, List<String> messages, String firstMsg) {
        this.errorSpans = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            int[] s = spans.get(i);
            String msg = i < messages.size() ? messages.get(i) : firstMsg;
            if (s[0] >= 0) errorSpans.add(new ErrorSpan(s[0], s[1], s[2], msg));
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
        this.errorMsg   = null;
        this.warnMode   = false;
        this.errorScroll = 0;
    }

    private boolean isErrorLine(int li) {
        for (ErrorSpan es : errorSpans) if (es.line() == li) return true;
        return false;
    }

    private String getActiveErrorMsg() {
        if (errorSpans.isEmpty()) return null;
        int selLo = Math.min(cursor, selAnchor);
        int selHi = Math.max(cursor, selAnchor);
        List<String> lines = getLines();
        int[] offs = buildLineOffsets(lines);
        for (ErrorSpan es : errorSpans) {
            if (es.line() < 0 || es.line() >= lines.size()) continue;
            int spanStart = offs[es.line()] + es.colStart();
            int spanEnd   = spanStart + Math.max(1, es.tokenLen());
            if (selLo < spanEnd && selHi >= spanStart) return es.message();
        }
        int[] curLC = offsetToLC(Math.max(selLo, cursor - 1), lines);
        for (ErrorSpan es : errorSpans) {
            if (es.line() == curLC[0]) return es.message();
        }
        return null;
    }

    private int errorBarH() {
        if (errorMsg == null) return 0;
        Font font = Minecraft.getInstance().font;
        List<String> wrapped = wrapErrorMsg(font, width - 8, errorMsg);
        int shownLines = Math.min(wrapped.size(), errorBarMaxLines());
        return Math.max(ERROR_BAR_H_MIN, shownLines * 10 + 4);
    }

    private List<String> wrapErrorMsg(Font font, int maxW, String msg) {
        String line = firstMeaningfulLine(msg);
        List<String> result = new ArrayList<>();
        String[] words = line.split(" ", -1);
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
            } else {
                String candidate = current + " " + word;
                if (font.width(candidate) > maxW) {
                    result.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current.append(" ").append(word);
                }
            }
        }
        if (!current.isEmpty()) result.add(current.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }

    @Override
    protected void renderContent(Gfx g, int mouseX, int mouseY, float pt) {
        Font font  = Minecraft.getInstance().font;
        int  x0    = getX(), y0 = getY();
        int  errorH = errorBarH();
        int  codeH  = height - errorH;
        UITheme t = NeoHouseConfig.get().uiTheme;

        g.fill(x0, y0, x0 + width, y0 + codeH, 0xE01E1E1E);
        g.fill(x0, y0, x0 + GUTTER_W, y0 + codeH, 0xE0252526);
        g.fill(x0 + GUTTER_W, y0, x0 + GUTTER_W + 1, y0 + codeH, t.accent());

        List<String> lines = getLines();
        int visLines = visibleLines();
        scrollLine = clampScrollLine(scrollLine, lines.size(), visLines);
        scrollX = Math.min(scrollX, maxScrollX());
        int[] lineOffsets = buildLineOffsets(lines);

        int[] curLC  = offsetToLC(cursor, lines);
        boolean hasSel = cursor != selAnchor;

        int codeAreaRight = x0 + width - SB_W - SB_PAD * 2;
        int endLine = Math.min(scrollLine + visLines, lines.size());

        g.enableScissor(x0 + GUTTER_W + 1, y0, codeAreaRight, y0 + codeH);

        for (int li = scrollLine; li < endLine; li++) {
            int ry = y0 + PAD_Y + (li - scrollLine) * LINE_H;
            String line = lines.get(li);
            int lineStart = lineOffsets[li];

            String activeFunc = (isFocused() && li == curLC[0]) ? paramHintFunc : null;
            int activeIdx  = (isFocused() && li == curLC[0]) ? paramHintIndex : -1;
            List<InlineHint> lineHints = computeLineHints(lines, li, activeFunc, activeIdx);

            if (isErrorLine(li)) {
                g.fill(x0 + GUTTER_W + 1, ry - 1, x0 + width, ry + LINE_H - 1, warnMode ? 0x44FFCC33 : 0x44FF3333);
            }

            if (fr.mode > 0 && !fr.matches.isEmpty() && !fr.query.isEmpty()) {
                int qLen = fr.query.length();
                for (int mi = 0; mi < fr.matches.size(); mi++) {
                    int matchOff = fr.matches.get(mi);
                    int matchEnd = matchOff + qLen;
                    int rowEnd   = lineStart + line.length();
                    if (matchEnd <= lineStart || matchOff >= rowEnd) continue;
                    int hi0 = Math.max(matchOff, lineStart);
                    int hi1 = Math.min(matchEnd, rowEnd);
                    int sx = codeX() + lineColToPixelX(font, line, hi0 - lineStart, lineHints) - scrollX;
                    int ex = codeX() + lineColToPixelX(font, line, hi1 - lineStart, lineHints) - scrollX;
                    int color = (mi == fr.matchIndex) ? 0x88FFAA00 : 0x55CCAA00;
                    g.fill(sx, ry - 1, ex, ry + LINE_H - 1, color);
                }
            }

            if (hasSel) {
                int selLo = Math.min(cursor, selAnchor);
                int selHi = Math.max(cursor, selAnchor);
                int rowEnd = lineStart + line.length();
                int hi0 = Math.max(selLo, lineStart);
                int hi1 = Math.min(selHi, rowEnd);
                if (hi0 < hi1) {
                    int sx = codeX() + lineColToPixelX(font, line, hi0 - lineStart, lineHints) - scrollX;
                    int ex = codeX() + lineColToPixelX(font, line, hi1 - lineStart, lineHints) - scrollX;
                    g.fill(sx, ry - 1, ex, ry + LINE_H - 1, 0x664477AA);
                }
            }

            renderHighlightedLine(g, font, line, lineStart, codeX() - scrollX, ry, lineHints);

            for (ErrorSpan es : errorSpans) {
                if (es.line() == li && es.colStart() >= 0 && es.tokenLen() > 0) {
                    int uLo = Math.min(es.colStart(), line.length());
                    int uHi = Math.min(es.colStart() + es.tokenLen(), line.length());
                    if (uHi > uLo) {
                        int ux0 = codeX() + lineColToPixelX(font, line, uLo, lineHints) - scrollX;
                        int ux1 = codeX() + lineColToPixelX(font, line, uHi, lineHints) - scrollX;
                        int uy  = ry + LINE_H - 2;
                        int uCol = warnMode ? 0xFFFFAA33 : 0xFFFF4444;
                        for (int wx = ux0; wx < ux1; wx++) {
                            g.fill(wx, uy + (wx % 2), wx + 1, uy + (wx % 2) + 1, uCol);
                        }
                    }
                }
            }

            if (isFocused() && li == curLC[0]) {
                int col = Math.min(curLC[1], line.length());
                int cx = codeX() + lineColToPixelX(font, line, col, lineHints) - scrollX;
                if ((System.currentTimeMillis() / 530) % 2 == 0) {
                    g.fill(cx, ry - 1, cx + 1, ry + LINE_H - 1, 0xFFCCCCCC);
                }
            }
        }

        g.disableScissor();

        for (int li = scrollLine; li < endLine; li++) {
            int ry = y0 + PAD_Y + (li - scrollLine) * LINE_H;
            String num = String.valueOf(li + 1);
            g.drawString(font, num,
                    x0 + GUTTER_W - font.width(num) - 3, ry,
                    isErrorLine(li) ? (warnMode ? 0xFFFFCC66 : 0xFFFF6666) : 0xFF636363, false);
        }

        renderScrollbar(g, x0, y0, codeH, lines.size(), visLines, t);
        renderHorizontalScrollbar(g, codeH, t);

        if (errorMsg != null) {
            String displayMsg = getActiveErrorMsg();
            if (displayMsg == null) displayMsg = errorMsg;
            int ey = y0 + codeH;
            int barBg   = warnMode ? 0xEE3B3111 : 0xEE3B1111;
            int barLine = warnMode ? 0xFFAAAA33 : 0xFFAA3333;
            int barText = warnMode ? 0xFFFFDD88 : 0xFFFF8888;
            g.fill(x0, ey, x0 + width, ey + errorH, barBg);
            g.fill(x0, ey, x0 + width, ey + 1,      barLine);
            List<String> wrapped = wrapErrorMsg(font, width - 8, displayMsg);
            int maxScroll = Math.max(0, wrapped.size() - errorBarMaxLines());
            errorScroll = Math.max(0, Math.min(errorScroll, maxScroll));
            int shownLines = Math.min(wrapped.size(), errorBarMaxLines());
            for (int i = 0; i < shownLines; i++) {
                int wi = errorScroll + i;
                if (wi >= wrapped.size()) break;
                g.drawString(font, wrapped.get(wi), x0 + 4, ey + 3 + i * 10, barText, false);
            }
            if (errorScroll > 0)         g.drawString(font, "▲", x0 + width - 10, ey + 3, 0xFFFFCC66, false);
            if (errorScroll < maxScroll) g.drawString(font, "▼", x0 + width - 10, ey + errorH - 11, 0xFFFFCC66, false);
        }

        int border = t.accent();
        g.fill(x0, y0, x0 + width, y0 + 1, border);
        g.fill(x0, y0 + height - 1, x0 + width, y0 + height, border);
        g.fill(x0, y0, x0 + 1, y0 + height, border);
        g.fill(x0 + width - 1, y0, x0 + width, y0 + height, border);

        renderFindBar(g, font);
        renderSuggestions(g, font, x0, y0, lines);
    }

    private void renderScrollbar(Gfx g, int x0, int y0, int codeH, int totalLines, int visLines, UITheme t) {
        if (totalLines <= visLines) return;
        int trackX = x0 + width - SB_W - SB_PAD;
        g.fill(trackX, y0, trackX + SB_W, y0 + codeH, 0x33FFFFFF);
        int maxOff = totalLines - visLines;
        int thumbH = Math.max(8, codeH * visLines / totalLines);
        int thumbY = y0 + (maxOff > 0 ? (codeH - thumbH) * scrollLine / maxOff : 0);
        int color = sbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, color);
    }

    private void renderHorizontalScrollbar(Gfx g, int codeH, UITheme t) {
        int maxScroll = maxScrollX();
        if (maxScroll <= 0) return;
        int[] track = hsbTrack(codeH);
        int thumbW = hsbThumbWidth(track[2]);
        int thumbX = track[0] + (track[2] - thumbW) * scrollX / maxScroll;
        g.fill(track[0], track[1], track[0] + track[2], track[1] + HSB_H, 0x33FFFFFF);
        int color = hsbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(thumbX, track[1], thumbX + thumbW, track[1] + HSB_H, color);
    }

    /** {x, y, width} of the horizontal scrollbar track along the bottom of the code area. */
    private int[] hsbTrack(int codeH) {
        int left = getX() + GUTTER_W + 1;
        int right = getX() + width - SB_W - SB_PAD * 2;
        return new int[]{left, getY() + codeH - HSB_H - 1, right - left};
    }

    private int hsbThumbWidth(int trackW) {
        int viewW = codeViewWidth();
        return Math.max(HSB_MIN_THUMB_W, trackW * viewW / Math.max(viewW, contentWidth()));
    }

    /** Width available for code text between the gutter and the vertical scrollbar. */
    private int codeViewWidth() {
        return Math.max(20, width - GUTTER_W - 1 - PAD_X * 2 - SB_W - SB_PAD * 2);
    }

    private int contentWidth() {
        if (contentWidthCache >= 0) return contentWidthCache;
        List<String> lines = getLines();
        int widest = 0;
        for (int i = 1; i < lines.size(); i++) if (lines.get(i).length() > lines.get(widest).length()) widest = i;
        String line = lines.get(widest);
        Font font = Minecraft.getInstance().font;
        contentWidthCache = lineColToPixelX(font, line, line.length(), computeLineHints(lines, widest, null, -1)) + PAD_X;
        return contentWidthCache;
    }

    private int maxScrollX() {
        return Math.max(0, contentWidth() - codeViewWidth());
    }

    private int codeX() { return getX() + GUTTER_W + 1 + PAD_X; }

    private static String firstMeaningfulLine(String s) {
        for (String part : s.split("\n")) {
            String t = part.trim();
            if (!t.isEmpty()) return t;
        }
        return s.trim();
    }

    private List<InlineHint> computeLineHints(List<String> allLines, int lineIndex, String activeFuncName, int activeParamIdx) {
        if (!NeoHouseConfig.get().showArgumentHints) return List.of();

        String line = allLines.get(lineIndex);
        List<InlineHint> hints = new ArrayList<>();

        int functionDefParenPos = -1;
        int fi = line.indexOf("function");
        if (fi >= 0) {
            int k = fi + "function".length();
            while (k < line.length() && line.charAt(k) == ' ') k++;
            while (k < line.length() && (Character.isLetterOrDigit(line.charAt(k)) || line.charAt(k) == '_')) k++;
            if (k < line.length() && line.charAt(k) == ' ') {
                while (k < line.length() && line.charAt(k) == ' ') k++;
                while (k < line.length() && (Character.isLetterOrDigit(line.charAt(k)) || line.charAt(k) == '_')) k++;
            }
            while (k < line.length() && line.charAt(k) == ' ') k++;
            if (k < line.length() && line.charAt(k) == '(') functionDefParenPos = k;
        }

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
            if (c == '"') {
                i++;
                while (i < line.length() && line.charAt(i) != '"') i++;
                if (i < line.length()) i++;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int nameStart = i;
                while (i < line.length() && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_'))
                    i++;
                int j = i;
                while (j < line.length() && line.charAt(j) == ' ') j++;
                if (j < line.length() && line.charAt(j) == '(' && j != functionDefParenPos) {
                    String funcName = line.substring(nameStart, i);
                    List<String> paramLabels = resolveParamNames(funcName);
                    if (paramLabels != null && !paramLabels.isEmpty()) {
                        List<Integer> argStarts = findArgStartsInLine(line, j + 1);
                        for (int k = 0; k < Math.min(argStarts.size(), paramLabels.size()); k++) {
                            String label = paramLabels.get(k);
                            int colon = label.indexOf(':');
                            String name = (colon >= 0 ? label.substring(0, colon) : label).trim();
                            boolean isActive = funcName.equals(activeFuncName) && k == activeParamIdx;
                            hints.add(new InlineHint(argStarts.get(k), name + ": ", isActive));
                        }
                    }
                    i = j + 1;
                    continue;
                }
                continue;
            }
            i++;
        }

        List<InlineHint> contHints = computeContinuationHints(allLines, lineIndex, activeFuncName, activeParamIdx);
        if (!contHints.isEmpty()) {
            hints.addAll(contHints);
            hints.sort(Comparator.comparingInt(InlineHint::charOffset));
        }

        return hints;
    }

    /**
     * Hints for a line that continues a call's argument list from an earlier line. Only applies when
     * the previous code ends with ',' or '(' inside a still-open call and this line doesn't hold a
     * statement of its own, and the call's ')' follows before the statement ends, so an unfinished
     * call being typed doesn't label the lines below it.
     * Scans a bounded window of earlier lines, skipping strings and comments.
     */
    private List<InlineHint> computeContinuationHints(List<String> allLines, int lineIndex, String activeFuncName, int activeParamIdx) {
        if (lineIndex == 0) return List.of();

        // Open brackets as {bracket char, line, column, argument index}, innermost on top.
        Deque<int[]> open = new ArrayDeque<>();
        char lastSignificant = 0;
        for (int li = Math.max(0, lineIndex - CONTINUATION_SCAN_LINES); li < lineIndex; li++) {
            String line = allLines.get(li);
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') break;
                if (c == '"') {
                    i++;
                    while (i < line.length() && line.charAt(i) != '"') i++;
                    lastSignificant = '"';
                    continue;
                }
                if (Character.isWhitespace(c)) continue;
                lastSignificant = c;
                switch (c) {
                    case '(', '[', '{' -> open.push(new int[]{c, li, i, 0});
                    case ')', ']', '}' -> { if (!open.isEmpty()) open.pop(); }
                    case ',' -> { if (!open.isEmpty()) open.peek()[3]++; }
                    case ';' -> open.clear(); // a finished statement: nothing before it is still open
                    default -> { }
                }
            }
        }
        if (open.isEmpty() || open.peek()[0] != '(' || (lastSignificant != ',' && lastSignificant != '(')) return List.of();
        if (!CallSpan.closesBeforeStatementEnds(allLines, lineIndex, CONTINUATION_SCAN_LINES)) return List.of();

        int[] call = open.peek();
        String callLine = allLines.get(call[1]);
        int nameEnd = call[2];
        while (nameEnd > 0 && callLine.charAt(nameEnd - 1) == ' ') nameEnd--;
        int nameStart = nameEnd;
        while (nameStart > 0 && (Character.isLetterOrDigit(callLine.charAt(nameStart - 1)) || callLine.charAt(nameStart - 1) == '_'))
            nameStart--;
        if (nameStart >= nameEnd) return List.of();
        String funcName = callLine.substring(nameStart, nameEnd);
        List<String> paramLabels = resolveParamNames(funcName);
        if (paramLabels == null || paramLabels.isEmpty()) return List.of();

        int argIndex = call[3];
        List<InlineHint> hints = new ArrayList<>();
        String curLine = allLines.get(lineIndex);
        int col = 0;
        while (col < curLine.length() && curLine.charAt(col) == ' ') col++;
        if (col < curLine.length() && curLine.charAt(col) != ')' && argIndex < paramLabels.size())
            hints.add(paramHint(paramLabels, argIndex, col, funcName, activeFuncName, activeParamIdx));

        int depth = 0;
        while (col < curLine.length()) {
            char c = curLine.charAt(col);
            if (c == '/' && col + 1 < curLine.length() && curLine.charAt(col + 1) == '/') break;
            if (c == '"') {
                col++;
                while (col < curLine.length() && curLine.charAt(col) != '"') col++;
                if (col < curLine.length()) col++;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
                col++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) break;
                depth--;
                col++;
            } else if (c == ';') {
                return List.of(); // a statement of its own, not part of the call
            } else if (c == ',' && depth == 0) {
                argIndex++;
                col++;
                while (col < curLine.length() && curLine.charAt(col) == ' ') col++;
                if (col < curLine.length() && curLine.charAt(col) != ')' && argIndex < paramLabels.size())
                    hints.add(paramHint(paramLabels, argIndex, col, funcName, activeFuncName, activeParamIdx));
            } else {
                col++;
            }
        }
        return hints;
    }

    private static InlineHint paramHint(List<String> paramLabels, int argIndex, int col,
                                        String funcName, String activeFuncName, int activeParamIdx) {
        String label = paramLabels.get(argIndex);
        int colon = label.indexOf(':');
        String name = (colon >= 0 ? label.substring(0, colon) : label).trim();
        return new InlineHint(col, name + ": ", funcName.equals(activeFuncName) && argIndex == activeParamIdx);
    }

    private List<Integer> findArgStartsInLine(String line, int start) {
        List<Integer> result = new ArrayList<>();
        int i = start;
        int depth = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        if (i < line.length() && line.charAt(i) != ')') result.add(i);
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') {
                i++;
                while (i < line.length() && line.charAt(i) != '"') i++;
                if (i < line.length()) i++;
            } else if (c == '(' || c == '[' || c == '{') {
                depth++; i++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) break;
                depth--; i++;
            } else if (c == ',' && depth == 0) {
                i++;
                while (i < line.length() && line.charAt(i) == ' ') i++;
                if (i < line.length() && line.charAt(i) != ')') result.add(i);
            } else {
                i++;
            }
        }
        return result;
    }

    private int lineColToPixelX(Font font, String line, int col, List<InlineHint> hints) {
        int charPos = 0;
        int pixW    = 0;
        for (InlineHint h : hints) {
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

    private int pixelXToLineCol(Font font, String line, int relX, List<InlineHint> hints) {
        int charPos = 0;
        int pixConsumed = 0;
        for (InlineHint h : hints) {
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

    private void renderHighlightedLine(Gfx g, Font font,
                                       String line, int lineStart,
                                       int x, int y, List<InlineHint> hints) {
        int lineEnd   = lineStart + line.length();
        int curX      = x;
        int charDrawn = 0;
        int hintPtr   = 0;

        // Spans are sorted and non-overlapping, so jump straight to the first one touching this
        // line instead of scanning from the start (which made deep scrolling O(spans) per line).
        for (int si = firstSpanTouching(lineStart); si < spans.size(); si++) {
            SyntaxHighlighter.Span sp = spans.get(si);
            if (sp.start() >= lineEnd)  break;

            int lo = Math.max(0, Math.min(sp.start() - lineStart, line.length()));
            int hi = Math.max(lo, Math.min(sp.end() - lineStart, line.length()));
            lo = Math.max(lo, charDrawn);

            if (lo > charDrawn) {
                int[] r = drawSegment(g, font, line, charDrawn, lo, 0xFFD4D4D4, hints, hintPtr, curX, y);
                curX = r[0]; charDrawn = r[1]; hintPtr = r[2];
            }

            if (hi > charDrawn) {
                int spanColor = sp.color() == 0 ? 0xFFD4D4D4 : sp.color();
                int[] r = drawSegment(g, font, line, charDrawn, hi, spanColor, hints, hintPtr, curX, y);
                curX = r[0]; charDrawn = r[1]; hintPtr = r[2];
            }
        }

        if (charDrawn < line.length()) {
            drawSegment(g, font, line, charDrawn, line.length(), 0xFFD4D4D4, hints, hintPtr, curX, y);
        }
    }

    private int[] drawSegment(Gfx g, Font font, String line,
                               int from, int to, int color,
                               List<InlineHint> hints, int hintPtr,
                               int curX, int y) {
        int pos = from;
        while (hintPtr < hints.size()) {
            InlineHint hint = hints.get(hintPtr);
            int hOff = hint.charOffset();
            if (hOff >= to) break;
            if (hOff < from) { hintPtr++; continue; }

            if (hOff > pos) {
                String seg = line.substring(pos, hOff);
                g.drawString(font, seg, curX, y, color, false);
                curX += font.width(seg);
                pos = hOff;
            }
            String ht      = hint.displayText();
            UITheme hintTheme = NeoHouseConfig.get().uiTheme;
            int    htColor = hint.isActive() ? hintTheme.muted() : hintTheme.divider();
            g.drawString(font, Component.literal(ht).withStyle(ChatFormatting.ITALIC), curX, y, htColor, false);
            curX += font.width(ht);
            hintPtr++;
        }
        if (to > pos && pos <= line.length()) {
            int drawEnd = Math.min(to, line.length());
            if (drawEnd > pos) {
                String seg = line.substring(pos, drawEnd);
                g.drawString(font, seg, curX, y, color, false);
                curX += font.width(seg);
            }
        }
        return new int[]{curX, to, hintPtr};
    }

    private void rebuildSpans() {
        spans = highlighter.buildSpans(text.toString());
    }

    /** Index of the first span whose end is past {@code lineStart} (spans are sorted by end). */
    private int firstSpanTouching(int lineStart) {
        int lo = 0, hi = spans.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (spans.get(mid).end() <= lineStart) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        boolean ctrl  = (mods & Keys.MOD_CONTROL) != 0;
        boolean shift = (mods & Keys.MOD_SHIFT)   != 0;

        // The find bar only takes keys while one of its fields has focus; clicking the code gives
        // the editor its shortcuts (Ctrl+A, Ctrl+C, ...) back without closing the bar.
        if (fr.mode > 0 && fr.focused != FOCUS_CODE && handleFindBarKey(key, mods)) return true;

        if (ctrl && key == Keys.F) { openFind(); return true; }
        if (ctrl && key == Keys.R) { openReplace(); return true; }

        if (!ctrl && !shift && activeSuggestions != null && activeSuggestions.length > 0) {
            if (key == Keys.TAB)    { applySuggestion(); return true; }
            if (key == Keys.UP)     { suggestionIndex = (suggestionIndex - 1 + activeSuggestions.length) % activeSuggestions.length; clampSuggestionScroll(); return true; }
            if (key == Keys.DOWN)   { suggestionIndex = (suggestionIndex + 1) % activeSuggestions.length; clampSuggestionScroll(); return true; }
            if (key == Keys.ESCAPE) { dismissSuggestions(); return true; }
        }

        if (ctrl) {
            switch (key) {
                case Keys.A -> { selectAll();           return true; }
                case Keys.C -> { copySelection();       return true; }
                case Keys.X -> { cutSelection();        return true; }
                case Keys.V -> { pasteClipboard();      return true; }
                case Keys.Z -> { if (shift) redo(); else undo(); return true; }
                case Keys.Y -> { redo();                return true; }
                case Keys.HOME  -> { moveCursor(0,             shift); return true; }
                case Keys.END   -> { moveCursor(text.length(), shift); return true; }
                case Keys.LEFT      -> { moveWordLeft(shift);       return true; }
                case Keys.RIGHT     -> { moveWordRight(shift);      return true; }
                case Keys.BACKSPACE -> { deleteWordBackward();       return true; }
            }
        }

        switch (key) {
            case Keys.TAB -> {
                if (hasSelection()) {
                    if (shift) dedentSelection(); else indentSelection();
                } else {
                    if (shift) dedent(); else insertString(" ".repeat(spacesPerTab()));
                }
                return true;
            }
            case Keys.BACKSPACE -> { deleteBackward(); return true; }
            case Keys.DELETE    -> { deleteForward();  return true; }
            case Keys.ENTER,
                 Keys.KP_ENTER  -> { insertChar('\n'); return true; }
            case Keys.LEFT      -> { moveHoriz(-1, shift); return true; }
            case Keys.RIGHT     -> { moveHoriz( 1, shift); return true; }
            case Keys.UP        -> { moveVert(-1, shift);  return true; }
            case Keys.DOWN      -> { moveVert( 1, shift);  return true; }
            case Keys.HOME      -> { moveToLineEdge(true,  shift); return true; }
            case Keys.END       -> { moveToLineEdge(false, shift); return true; }
            case Keys.PAGE_UP   -> { moveVert(-visibleLines(), shift); return true; }
            case Keys.PAGE_DOWN -> { moveVert( visibleLines(), shift); return true; }
        }
        return false;
    }

    @Override
    protected boolean onCharTyped(char c, int mods) {
        if (c < 32 || c == 127) return false;
        if (fr.mode > 0 && fr.focused != FOCUS_CODE) return handleFindBarChar(c);
        insertChar(c);
        return true;
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) { setFocused(false); activeSuggestions = null; return false; }

        if (fr.mode == 2 && btn == 0) {
            int btnH = 11;
            if (fr.btnReplaceX >= 0 && mx >= fr.btnReplaceX && mx < fr.btnReplaceX + FIND_BTN_W
                    && my >= fr.btnReplaceY && my < fr.btnReplaceY + btnH) {
                replaceOne(); return true;
            }
            if (fr.btnReplaceAllX >= 0 && mx >= fr.btnReplaceAllX && mx < fr.btnReplaceAllX + FIND_BTN_W + 10
                    && my >= fr.btnReplaceAllY && my < fr.btnReplaceAllY + btnH) {
                replaceAll(); return true;
            }
        }

        if (fr.mode > 0 && btn == 0) {
            Font font = Minecraft.getInstance().font;
            int x0 = getX(), y0 = getY();
            int bx = x0 + width - FIND_BAR_W - 2;
            int by = y0 + 2;
            String countLabel = fr.query.isEmpty() ? "" :
                    fr.matches.isEmpty() ? "no matches" : (fr.matchIndex + 1) + "/" + fr.matches.size();
            int labelW = countLabel.isEmpty() ? 0 : font.width(countLabel) + 4;
            int fieldW = FIND_BAR_W - 12 - labelW;
            int fy = by + 4;
            int fx = bx + 4;
            if (mx >= fx && mx < fx + fieldW && my >= fy && my < fy + FIND_FIELD_H) {
                fr.focused = 0;
                int col = findFieldClickToCol(font, fr.query, (int)(mx - fx - 2), fr.cursor);
                fr.cursor = col; fr.selAnchor = col;
                return true;
            }
            if (fr.mode == 2) {
                int ry = fy + FIND_FIELD_H + 4;
                if (mx >= fx && mx < fx + fieldW && my >= ry && my < ry + FIND_FIELD_H) {
                    fr.focused = 1;
                    int col = findFieldClickToCol(font, fr.replace, (int)(mx - fx - 2), fr.replaceCursor);
                    fr.replaceCursor = col; fr.replaceSelAnchor = col;
                    return true;
                }
            }
        }

        int x0 = getX(), y0 = getY();
        int errorH = errorBarH();
        int codeH  = height - errorH;
        List<String> lines = getLines();
        int visLines = visibleLines();

        int maxScroll = maxScrollX();
        if (maxScroll > 0 && btn == 0) {
            int[] track = hsbTrack(codeH);
            if (mx >= track[0] && mx < track[0] + track[2] && my >= track[1] - 2 && my <= track[1] + HSB_H + 2) {
                int thumbW = hsbThumbWidth(track[2]);
                int thumbX = track[0] + (track[2] - thumbW) * scrollX / maxScroll;
                if (mx < thumbX || mx >= thumbX + thumbW) {
                    int slideW = Math.max(1, track[2] - thumbW);
                    scrollX = Math.max(0, Math.min(maxScroll, (int) ((mx - track[0] - thumbW / 2.0) * maxScroll / slideW)));
                }
                hsbDragging = true;
                hsbDragStartX = (int) mx;
                hsbDragStartScroll = scrollX;
                return true;
            }
        }

        if (lines.size() > visLines) {
            int trackX = x0 + width - SB_W - SB_PAD;
            if (mx >= trackX - 2 && mx <= trackX + SB_W + 2 && my >= y0 && my < y0 + codeH) {
                int maxOff = lines.size() - visLines;
                int thumbH = Math.max(8, codeH * visLines / lines.size());
                int thumbY = y0 + (codeH - thumbH) * scrollLine / maxOff;
                if (my >= thumbY && my < thumbY + thumbH) {
                    sbDragging            = true;
                    sbDragStartY          = (int) my;
                    sbDragStartScrollLine = scrollLine;
                } else {
                    int newScroll = (int) ((my - y0 - thumbH / 2.0) * maxOff / Math.max(1, codeH - thumbH));
                    scrollLine = Math.max(0, Math.min(maxOff, newScroll));
                }
                return true;
            }
        }

        if (activeSuggestions != null && suggPanelX >= 0
                && mx >= suggPanelX && mx < suggPanelX + suggPanelW
                && my >= suggPanelY && my < suggPanelY + suggPanelH) {
            int row = ((int) my - suggPanelY - 2) / LINE_H;
            int si  = row + suggestionScrollOffset;
            if (si >= 0 && si < activeSuggestions.length) {
                long now = System.currentTimeMillis();
                boolean dbl = (now - lastClickTime < 500) && (si == suggestionIndex);
                lastClickTime = now;
                lastClickPos  = -1;
                if (dbl) {
                    applySuggestion();
                } else {
                    suggestionIndex = si;
                }
            }
            return true;
        }

        setFocused(true);
        fr.focused = FOCUS_CODE;
        int pos = hitToOffset((int) mx, (int) my);
        if (pos >= 0) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = (now - lastClickTime < 500) && (pos == lastClickPos);
            lastClickTime = now;
            lastClickPos  = pos;

            if (RenderHelper.hasShiftDown()) {
                cursor = pos; // Shift+click extends the selection from the current anchor
            } else if (isDoubleClick) {
                selectWordAt(pos);
            } else {
                cursor    = pos;
                selAnchor = pos;
            }
        }
        dismissSuggestions();
        computeParamHint();
        return true;
    }

    @Override
    protected boolean onMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (hsbDragging && btn == 0) {
            int[] track = hsbTrack(height - errorBarH());
            int slideW = track[2] - hsbThumbWidth(track[2]);
            if (slideW > 0)
                scrollX = Math.max(0, Math.min(maxScrollX(), hsbDragStartScroll + (int) (mx - hsbDragStartX) * maxScrollX() / slideW));
            return true;
        }
        if (sbDragging && btn == 0) {
            int errorH = errorBarH();
            int codeH  = height - errorH;
            List<String> lines = getLines();
            int visLines = visibleLines();
            int thumbH   = Math.max(8, codeH * visLines / lines.size());
            int maxOff   = Math.max(0, lines.size() - visLines);
            int slideH   = codeH - thumbH;
            if (slideH > 0) {
                int delta = (int) (my - sbDragStartY);
                scrollLine = Math.max(0, Math.min(maxOff, sbDragStartScrollLine + delta * maxOff / slideH));
            }
            return true;
        }

        if (btn != 0 || !isFocused()) return false;
        int pos = hitToOffset((int) mx, (int) my);
        if (pos >= 0) {
            cursor = Math.max(0, Math.min(pos, text.length()));
            ensureCursorVisible();
        }
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mx, double my, int btn) {
        if (hsbDragging && btn == 0) { hsbDragging = false; return true; }
        if (sbDragging && btn == 0) { sbDragging = false; return true; }
        return superMouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!isMouseOver(mx, my)) return false;

        if (errorMsg != null) {
            int errorH = errorBarH();
            int barTop = getY() + height - errorH;
            if (my >= barTop && my < getY() + height) {
                errorScroll -= (int) Math.signum(dy);
                return true;
            }
        }

        if (activeSuggestions != null && activeSuggestions.length > MAX_VISIBLE_SUGGESTIONS
                && suggPanelX >= 0
                && mx >= suggPanelX && mx < suggPanelX + suggPanelW
                && my >= suggPanelY && my < suggPanelY + suggPanelH) {
            int maxOff = activeSuggestions.length - MAX_VISIBLE_SUGGESTIONS;
            suggestionScrollOffset = Math.max(0, Math.min(maxOff, suggestionScrollOffset - (int) Math.signum(dy)));
            return true;
        }

        if (RenderHelper.hasShiftDown() || dx != 0) {
            double amount = dx != 0 ? dx : dy;
            scrollX = Math.max(0, Math.min(maxScrollX(), scrollX - (int) Math.signum(amount) * H_SCROLL_STEP));
            return true;
        }
        scrollLine = clampScrollLine(scrollLine - (int) Math.signum(dy),
                getLines().size(), visibleLines());
        return true;
    }

    private void insertChar(char c) {
        String autoIndentStr = "";
        if (c == '\n' && NeoHouseConfig.get().autoIndent) {
            List<String> lines = getLines();
            int[] lc = offsetToLC(cursor, lines);
            String curLine = lines.get(lc[0]);
            int ws = 0;
            while (ws < curLine.length() && curLine.charAt(ws) == ' ') ws++;
            if (ws > 0) autoIndentStr = curLine.substring(0, ws);
        }

        pushUndo();
        if (hasSelection()) deleteSelectionInternal();
        text.insert(cursor, c);
        cursor++;
        selAnchor = cursor;

        if (!autoIndentStr.isEmpty()) {
            text.insert(cursor, autoIndentStr);
            cursor    += autoIndentStr.length();
            selAnchor  = cursor;
        }

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
        for (int i = 0; i < Math.min(spacesPerTab(), line.length()); i++) {
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

    private void indentSelection() {
        List<String> lines = getLines();
        int[] offs = buildLineOffsets(lines);
        int selLo = Math.min(cursor, selAnchor);
        int selHi = Math.max(cursor, selAnchor);

        int firstLine = offsetToLC(selLo, lines)[0];
        int[] hiLC = offsetToLC(selHi, lines);
        int lastLine = (hiLC[1] == 0 && hiLC[0] > firstLine) ? hiLC[0] - 1 : hiLC[0];

        String indent = " ".repeat(spacesPerTab());
        pushUndo();
        for (int li = lastLine; li >= firstLine; li--) {
            text.insert(offs[li], indent);
        }
        cursor    = shiftByLineIndent(cursor,    offs, firstLine, lastLine);
        selAnchor = shiftByLineIndent(selAnchor, offs, firstLine, lastLine);
        onTextChanged();
    }

    private int shiftByLineIndent(int pos, int[] offs, int firstLine, int lastLine) {
        int shift = 0;
        for (int li = firstLine; li <= lastLine; li++) {
            if (offs[li] < pos) shift += spacesPerTab();
            else break;
        }
        return Math.min(pos + shift, text.length());
    }

    private void dedentSelection() {
        List<String> lines = getLines();
        int[] offs = buildLineOffsets(lines);
        int selLo = Math.min(cursor, selAnchor);
        int selHi = Math.max(cursor, selAnchor);

        int firstLine = offsetToLC(selLo, lines)[0];
        int[] hiLC = offsetToLC(selHi, lines);
        int lastLine = (hiLC[1] == 0 && hiLC[0] > firstLine) ? hiLC[0] - 1 : hiLC[0];

        int[] removed = new int[lastLine - firstLine + 1];
        for (int li = firstLine; li <= lastLine; li++) {
            String lineStr = lines.get(li);
            int r = 0;
            for (int j = 0; j < Math.min(spacesPerTab(), lineStr.length()); j++) {
                if (lineStr.charAt(j) == ' ') r++;
                else break;
            }
            removed[li - firstLine] = r;
        }
        boolean anyRemoved = false;
        for (int r : removed) if (r > 0) { anyRemoved = true; break; }
        if (!anyRemoved) return;

        pushUndo();
        for (int li = lastLine; li >= firstLine; li--) {
            int r = removed[li - firstLine];
            if (r > 0) text.delete(offs[li], offs[li] + r);
        }
        cursor    = shiftByLineDedent(cursor,    offs, firstLine, lastLine, removed);
        selAnchor = shiftByLineDedent(selAnchor, offs, firstLine, lastLine, removed);
        onTextChanged();
    }

    private int shiftByLineDedent(int originalPos, int[] offs, int firstLine, int lastLine, int[] removed) {
        int totalRemovedBefore = 0;
        for (int li = firstLine; li <= lastLine; li++) {
            int r = removed[li - firstLine];
            int origLineOff = offs[li];
            if (originalPos < origLineOff) break;
            if (r == 0) continue;
            if (originalPos < origLineOff + r) {
                return origLineOff - totalRemovedBefore;
            }
            totalRemovedBefore += r;
        }
        return Math.max(0, originalPos - totalRemovedBefore);
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

    private void deleteWordBackward() {
        if (hasSelection()) { pushUndo(); deleteSelectionInternal(); onTextChanged(); return; }
        if (cursor <= 0) return;
        int start = wordLeftPos(cursor);
        if (start == cursor) return;
        pushUndo();
        text.delete(start, cursor);
        cursor    = start;
        selAnchor = cursor;
        onTextChanged();
    }

    private int wordLeftPos(int from) {
        int pos = from;
        if (pos <= 0) return 0;
        pos--;
        while (pos > 0 && isWordSeparator(text.charAt(pos))) pos--;
        if (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_') {
            while (pos > 0 && (Character.isLetterOrDigit(text.charAt(pos - 1)) || text.charAt(pos - 1) == '_'))
                pos--;
        } else {
            while (pos > 0 && !Character.isLetterOrDigit(text.charAt(pos - 1))
                   && !isWordSeparator(text.charAt(pos - 1)))
                pos--;
        }
        return pos;
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
        lineCache   = null;
        offsetCache = null;
        contentWidthCache = -1;
        rebuildSpans();
        if (fr.mode > 0) updateFindMatches();
        updateSuggestions();
        if (onChange != null) onChange.run();
    }

    private void moveCursor(int pos, boolean select) {
        pos    = Math.max(0, Math.min(pos, text.length()));
        cursor = pos;
        if (!select) selAnchor = pos;
        ensureCursorVisible();
        refreshOpenSuggestions();
    }

    private void moveHoriz(int dir, boolean select) {
        if (!select && hasSelection()) {
            cursor    = dir < 0 ? Math.min(cursor, selAnchor) : Math.max(cursor, selAnchor);
            selAnchor = cursor;
            ensureCursorVisible();
            refreshOpenSuggestions();
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

    private void moveWordLeft(boolean select) {
        moveCursor(wordLeftPos(cursor), select);
    }

    private void moveWordRight(boolean select) {
        int pos = cursor;
        int len = text.length();
        if (pos >= len) return;
        while (pos < len && isWordSeparator(text.charAt(pos))) pos++;
        if (pos < len) {
            if (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_') {
                while (pos < len && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_'))
                    pos++;
            } else {
                while (pos < len && !Character.isLetterOrDigit(text.charAt(pos)) && !isWordSeparator(text.charAt(pos)))
                    pos++;
            }
        }
        moveCursor(pos, select);
    }

    private static boolean isWordSeparator(char c) {
        return Character.isWhitespace(c) || c == '\n';
    }

    private void selectWordAt(int pos) {
        int len = text.length();
        if (len == 0) return;
        int p = Math.max(0, Math.min(pos, len - 1));
        char ch = text.charAt(p);
        if (Character.isLetterOrDigit(ch) || ch == '_') {
            int lo = p, hi = p;
            while (lo > 0 && (Character.isLetterOrDigit(text.charAt(lo - 1)) || text.charAt(lo - 1) == '_')) lo--;
            while (hi < len && (Character.isLetterOrDigit(text.charAt(hi)) || text.charAt(hi) == '_')) hi++;
            selAnchor = lo;
            cursor    = hi;
        } else {
            cursor    = pos;
            selAnchor = pos;
        }
    }

    private void selectAll() {
        selAnchor = 0;
        cursor    = text.length();
    }

    private void copySelection() {
        if (!hasSelection()) return;
        int lo = Math.min(cursor, selAnchor);
        int hi = Math.max(cursor, selAnchor);
        Minecraft.getInstance().keyboardHandler.setClipboard(text.substring(lo, hi));
    }

    private void cutSelection() {
        if (!hasSelection()) {
            List<String> lines = getLines();
            int[] lc   = offsetToLC(cursor, lines);
            int[] offs = buildLineOffsets(lines);
            int lineStart = offs[lc[0]];
            int lineEnd   = lineStart + lines.get(lc[0]).length();
            if (lineEnd < text.length()) lineEnd++; // include '\n'
            selAnchor = lineStart;
            cursor    = lineEnd;
        }
        copySelection();
        pushUndo();
        deleteSelectionInternal();
        onTextChanged();
    }

    private void pasteClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip.isEmpty()) return;
        clip = clip.replace("\r\n", "\n").replace("\r", "\n")
                   .replace("\t", " ".repeat(spacesPerTab()));
        insertString(clip);
    }

    private void pushUndo() {
        undoManager.push(text.toString(), cursor);
    }

    private void undo() {
        EditorUndoManager.EditorState state = undoManager.undo(text.toString(), cursor);
        if (state == null) return;
        text.setLength(0);
        text.append(state.text());
        cursor    = Math.min(state.cursor(), text.length());
        selAnchor = cursor;
        ensureCursorVisible();
        onTextChanged();
    }

    private void redo() {
        EditorUndoManager.EditorState state = undoManager.redo(text.toString(), cursor);
        if (state == null) return;
        text.setLength(0);
        text.append(state.text());
        cursor    = Math.min(state.cursor(), text.length());
        selAnchor = cursor;
        ensureCursorVisible();
        onTextChanged();
    }

    private void ensureCursorVisible() {
        List<String> lines = getLines();
        int[] lc  = offsetToLC(cursor, lines);
        int   vis = visibleLines();
        if (lc[0] < scrollLine)        scrollLine = lc[0];
        if (lc[0] >= scrollLine + vis) scrollLine = lc[0] - vis + 1;
        scrollLine = clampScrollLine(scrollLine, lines.size(), vis);

        Font font = Minecraft.getInstance().font;
        String line = lines.get(lc[0]);
        List<InlineHint> hints = computeLineHints(lines, lc[0], null, -1);
        int col      = Math.min(lc[1], line.length());
        int cursorPx = lineColToPixelX(font, line, col, hints);
        int viewW    = width - GUTTER_W - 1 - PAD_X * 2 - SB_W - SB_PAD * 2;
        if (viewW < 20) viewW = 20;
        if (cursorPx < scrollX)              scrollX = cursorPx;
        if (cursorPx > scrollX + viewW - 4)  scrollX = cursorPx - viewW + 4;
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX()));
    }

    private int clampScrollLine(int s, int lineCount, int vis) {
        return Math.max(0, Math.min(s, Math.max(0, lineCount - vis)));
    }

    /** Lines that fit in the code area, leaving room for the horizontal scrollbar when it shows. */
    private int visibleLines() {
        int codeH = height - errorBarH() - (maxScrollX() > 0 ? HSB_H + 1 : 0);
        return Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
    }

    private int hitToOffset(int mx, int my) {
        Font         font  = Minecraft.getInstance().font;
        List<String> lines = getLines();
        int[]        offs  = buildLineOffsets(lines);
        int li = scrollLine + (my - (getY() + PAD_Y)) / LINE_H;
        li = Math.max(0, Math.min(li, lines.size() - 1));
        String           line      = lines.get(li);
        List<InlineHint> lineHints = computeLineHints(lines, li, null, -1);
        int relX = mx - codeX() + scrollX;
        int col  = pixelXToLineCol(font, line, relX, lineHints);
        return offs[li] + col;
    }

    private List<String> getLines() {
        if (lineCache != null) return lineCache;
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
        lineCache = out;
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

    private int[] getCachedOffsets() {
        if (offsetCache != null) return offsetCache;
        offsetCache = buildLineOffsets(getLines());
        return offsetCache;
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

    public boolean hasSuggestions() {
        return activeSuggestions != null && activeSuggestions.length > 0;
    }

    public void dismissSuggestions() {
        activeSuggestions      = null;
        suggestionIndex        = 0;
        suggestionScrollOffset = 0;
        suggPanelX             = -1;
    }

    /** Recomputes the suggestion list while it's open (closing it if nothing matches); never opens it. */
    private void refreshOpenSuggestions() {
        if (activeSuggestions != null) updateSuggestions();
        else computeParamHint();
    }

    private void updateSuggestions() {
        String[] prev    = activeSuggestions;
        int      prevIdx = suggestionIndex;
        activeSuggestions = computeSuggestionsAtCursor();
        // Nothing to offer when the only suggestion is exactly what's already typed.
        if (activeSuggestions != null && activeSuggestions.length == 1 && !suggestionIsString
                && suggestionTypedLen <= cursor
                && text.substring(cursor - suggestionTypedLen, cursor).equals(activeSuggestions[0]))
            activeSuggestions = null;
        if (activeSuggestions == null) {
            suggestionIndex        = 0;
            suggestionScrollOffset = 0;
        } else if (prev == null || prev.length != activeSuggestions.length) {
            suggestionIndex        = 0;
            suggestionScrollOffset = 0;
        } else {
            suggestionIndex = Math.min(prevIdx, activeSuggestions.length - 1);
        }
        computeParamHint();
    }

    private String[] computeSuggestionsAtCursor() {
        suggestionTypedLen = 0;
        suggestionIsString = false;

        if (!NeoHouseConfig.get().showArgumentRecommendations) return null;

        List<String> lines = getLines();
        int[] lc = offsetToLC(cursor, lines);
        String lineText     = lines.get(lc[0]);
        String beforeCursor = lineText.substring(0, lc[1]);

        {
            String trimmed  = beforeCursor.trim();
            int    lastSpace = trimmed.lastIndexOf(' ');
            String lastWord  = lastSpace >= 0 ? trimmed.substring(lastSpace + 1) : trimmed;
            String beforeLast = lastSpace >= 0 ? trimmed.substring(0, lastSpace).trim() : "";
            if (beforeLast.equals("event")) {
                String[] matches = Arrays.stream(Event.values())
                        .map(e -> e.name)
                        .filter(n -> n.startsWith(lastWord))
                        .toArray(String[]::new);
                if (matches.length > 0) {
                    suggestionTypedLen = lastWord.length();
                    return matches;
                }
            }
        }

        {
            Matcher phm = PH_SUGGEST_PAT.matcher(beforeCursor);
            if (phm.find()) {
                String partial = phm.group(1);
                String[] matches = Arrays.stream(Placeholder.values())
                        .map(p -> p.name)
                        .filter(n -> n.startsWith(partial))
                        .toArray(String[]::new);
                if (matches.length > 0) {
                    suggestionTypedLen = partial.length();
                    return matches;
                }
            }
        }

        int quoteCount = 0;
        for (int i = 0; i < beforeCursor.length(); i++) if (beforeCursor.charAt(i) == '"') quoteCount++;

        // A bare location preset (teleport(INVOKER, ...)): suggest the preset names.
        if (quoteCount % 2 == 0) {
            Matcher word = TRAILING_WORD_PAT.matcher(beforeCursor);
            if (!word.find()) return null;
            String partial = word.group(1);
            String beforeWord = beforeCursor.substring(0, word.start(1));
            if (!beforeWord.stripTrailing().endsWith("(") && !beforeWord.stripTrailing().endsWith(",")) return null;
            Param param = parameterAt(beforeWord);
            if (param == null || param.kind() != ActionParamKind.LOCATION) return null;
            String[] presets = Arrays.stream(Teleport.values())
                    .filter(t -> t != Teleport.CUSTOM)
                    .map(Enum::name)
                    .filter(n -> n.startsWith(partial))
                    .toArray(String[]::new);
            if (presets.length == 0) return null;
            suggestionTypedLen = partial.length();
            return presets;
        }

        int quotePos   = beforeCursor.lastIndexOf('"');
        String typedIn = beforeCursor.substring(quotePos + 1);
        Param param = parameterAt(beforeCursor.substring(0, quotePos));
        String[] allowed = param == null ? null : param.enumValues();
        if (allowed == null) return null;

        final String prefix = typedIn;
        String[] filtered = Arrays.stream(allowed)
                                   .filter(s -> s.startsWith(prefix))
                                   .toArray(String[]::new);
        if (filtered.length == 0) return null;

        suggestionTypedLen = typedIn.length();
        suggestionIsString = true;
        return filtered;
    }

    /**
     * The action or condition parameter an argument starting right after {@code before} belongs to,
     * found by scanning back to the call's opening parenthesis; null outside a built-in call.
     */
    private static Param parameterAt(String before) {
        int paramIndex = 0, depth = 0, braceDepth = 0, funcNameEnd = -1;
        for (int i = before.length() - 1; i >= 0; i--) {
            char c = before.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && before.charAt(i) != '"') i--;
                continue;
            }
            if      (c == '}')                               braceDepth++;
            else if (c == '{')                               braceDepth--;
            else if (c == ')')                               depth++;
            else if (c == '(' && depth > 0)                  depth--;
            else if (c == '(')                               { funcNameEnd = i; break; }
            else if (c == ',' && depth == 0 && braceDepth == 0) paramIndex++;
        }
        if (funcNameEnd < 0) return null;

        String beforeParen = before.substring(0, funcNameEnd).stripTrailing();
        int nameStart = beforeParen.length();
        while (nameStart > 0 && Character.isLetterOrDigit(beforeParen.charAt(nameStart - 1))) nameStart--;
        String funcName = beforeParen.substring(nameStart);
        if (funcName.isEmpty()) return null;

        AddAction action = AddAction.searchByName(funcName);
        if (action != null) return paramIndex < action.params.size() ? action.params.get(paramIndex) : null;
        AddCondition cond = AddCondition.searchByName(funcName);
        if (cond != null) return paramIndex < cond.params.size() ? cond.params.get(paramIndex) : null;
        return null;
    }

    private void applySuggestion() {
        if (activeSuggestions == null || activeSuggestions.length == 0) return;
        String suggestion = activeSuggestions[Math.min(suggestionIndex, activeSuggestions.length - 1)];
        pushUndo();
        int deleteFrom = cursor - suggestionTypedLen;
        text.delete(deleteFrom, cursor);
        cursor = deleteFrom;
        String insert;
        if (suggestionIsString) {
            boolean needsClose = cursor >= text.length() || text.charAt(cursor) != '"';
            insert = suggestion + (needsClose ? "\"" : "");
        } else {
            insert = suggestion;
        }
        text.insert(cursor, insert);
        cursor   += insert.length();
        selAnchor = cursor;
        onTextChanged();
    }

    private void computeParamHint() {
        paramHintIndex = -1;
        paramHintFunc  = null;

        List<String> lines = getLines();
        int[] lc = offsetToLC(cursor, lines);
        String beforeCursor = lines.get(lc[0]).substring(0, lc[1]);

        int paramIndex = 0, depth = 0, braceDepth = 0, funcNameEnd = -1;
        for (int i = beforeCursor.length() - 1; i >= 0; i--) {
            char c = beforeCursor.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && beforeCursor.charAt(i) != '"') i--;
                continue;
            }
            if      (c == '}')                               braceDepth++;
            else if (c == '{')                               braceDepth--;
            else if (c == ')')                               depth++;
            else if (c == '(' && depth > 0)                  depth--;
            else if (c == '(')                               { funcNameEnd = i; break; }
            else if (c == ',' && depth == 0 && braceDepth == 0) paramIndex++;
        }
        if (funcNameEnd < 0) return;

        String beforeParen = beforeCursor.substring(0, funcNameEnd).stripTrailing();
        int nameStart = beforeParen.length();
        while (nameStart > 0 && (Character.isLetterOrDigit(beforeParen.charAt(nameStart - 1)) || beforeParen.charAt(nameStart - 1) == '_'))
            nameStart--;
        String funcName = beforeParen.substring(nameStart);
        if (funcName.isEmpty()) return;

        List<String> names = resolveParamNames(funcName);
        if (names == null || names.isEmpty()) return;

        paramHintFunc  = funcName;
        paramHintIndex = Math.min(paramIndex, names.size() - 1);
    }

    private List<String> resolveParamNames(String funcName) {
        AddAction action = AddAction.searchByName(funcName);
        if (action != null) {
            List<String> labels = new ArrayList<>();
            for (Param p : action.params) labels.add(p.hintLabel());
            return labels.isEmpty() ? null : labels;
        }
        AddCondition cond = AddCondition.searchByName(funcName);
        if (cond != null) {
            List<String> labels = new ArrayList<>();
            for (Param p : cond.params) labels.add(p.hintLabel());
            return labels.isEmpty() ? null : labels;
        }
        String[] phHints = PH_PARAM_HINTS.get(funcName);
        if (phHints != null) return Arrays.asList(phHints);
        return scanUserFunction(funcName);
    }

    private List<String> scanUserFunction(String funcName) {
        Matcher m = USER_FUNC_PAT.matcher(text);
        while (m.find()) {
            if (!m.group(1).equals(funcName)) continue;
            String ps = m.group(2).trim();
            if (ps.isEmpty()) return List.of();
            List<String> labels = new ArrayList<>();
            for (String part : ps.split(",")) {
                String[] toks = part.trim().split("\\s+");
                if (toks.length >= 2) labels.add(toks[toks.length - 1] + ": " + toks[0]);
                else if (toks.length == 1) labels.add(toks[0]);
            }
            return labels;
        }
        return null;
    }

    private void clampSuggestionScroll() {
        if (activeSuggestions == null) return;
        int maxOff = Math.max(0, activeSuggestions.length - MAX_VISIBLE_SUGGESTIONS);
        if (suggestionIndex < suggestionScrollOffset)
            suggestionScrollOffset = suggestionIndex;
        else if (suggestionIndex >= suggestionScrollOffset + MAX_VISIBLE_SUGGESTIONS)
            suggestionScrollOffset = suggestionIndex - MAX_VISIBLE_SUGGESTIONS + 1;
        suggestionScrollOffset = Math.max(0, Math.min(maxOff, suggestionScrollOffset));
    }

    private void renderSuggestions(Gfx g, Font font, int x0, int y0, List<String> lines) {
        UITheme t = NeoHouseConfig.get().uiTheme;
        if (activeSuggestions == null || activeSuggestions.length == 0 || !isFocused()) {
            suggPanelX = -1;
            return;
        }
        int[] curLC = offsetToLC(cursor, lines);
        if (curLC[0] < scrollLine || curLC[0] >= scrollLine + visibleLines()) {
            suggPanelX = -1;
            return;
        }

        String curLine = lines.get(curLC[0]);
        int    col     = Math.min(curLC[1], curLine.length());
        List<InlineHint> lineHints = computeLineHints(lines, curLC[0], null, -1);
        int    cx      = codeX() + lineColToPixelX(font, curLine, col, lineHints) - scrollX;
        int    cy      = y0 + PAD_Y + (curLC[0] - scrollLine) * LINE_H + LINE_H;

        int start  = suggestionScrollOffset;
        int end    = Math.min(activeSuggestions.length, start + MAX_VISIBLE_SUGGESTIONS);
        int maxVis = end - start;

        int panelW = 40;
        for (String s : activeSuggestions) panelW = Math.max(panelW, font.width(s) + 10);
        int panelH = maxVis * LINE_H + 4;

        int codeBottom = y0 + height - errorBarH();
        int px = Math.max(x0 + GUTTER_W + 1, Math.min(cx, x0 + width - panelW - 1));
        int py = (cy + panelH > codeBottom) ? Math.max(y0, cy - panelH - LINE_H) : cy;

        suggPanelX = px; suggPanelY = py; suggPanelW = panelW; suggPanelH = panelH;

        g.fill(px,          py,           px + panelW, py + panelH, t.dialog(0xF0));
        g.fill(px,          py,           px + panelW, py + 1,      t.accent());
        g.fill(px,          py + panelH - 1, px + panelW, py + panelH, t.panel(0xFF));
        g.fill(px,          py,           px + 1,      py + panelH, t.accent());
        g.fill(px + panelW - 1, py,       px + panelW, py + panelH, t.panel(0xFF));

        for (int si = start; si < end; si++) {
            int row = si - start;
            int sy  = py + 2 + row * LINE_H;
            if (si == suggestionIndex) g.fill(px + 1, sy - 1, px + panelW - 1, sy + LINE_H - 1, t.accentTint(0x55));
            g.drawString(font, activeSuggestions[si], px + 4, sy, 0xFFD4D4D4, false);
        }

        if (activeSuggestions.length > MAX_VISIBLE_SUGGESTIONS) {
            if (start > 0)
                g.drawString(font, "▲", px + panelW - 9, py + 2,             0xFF888888, false);
            if (end < activeSuggestions.length)
                g.drawString(font, "▼", px + panelW - 9, py + panelH - LINE_H, 0xFF888888, false);
        }
    }

    public void openFind() {
        fr.mode = 1;
        fr.focused = 0;
        fr.selAnchor = 0; fr.cursor = fr.query.length();
        updateFindMatches();
    }

    public void openReplace() {
        fr.mode = 2;
        fr.focused = 0;
        fr.selAnchor = 0; fr.cursor = fr.query.length();
        updateFindMatches();
    }

    public void closeFindBar() {
        fr.mode = 0;
        fr.matches.clear();
        fr.matchIndex = -1;
        fr.btnReplaceX = -1; fr.btnReplaceAllX = -1;
    }

    public boolean isFindOpen() { return fr.mode > 0; }

    private void updateFindMatches() {
        fr.matches.clear();
        fr.matchIndex = -1;
        if (fr.query.isEmpty()) return;
        String src = text.toString().toLowerCase();
        String q   = fr.query.toString().toLowerCase();
        int i = 0;
        while (i <= src.length() - q.length()) {
            int found = src.indexOf(q, i);
            if (found < 0) break;
            fr.matches.add(found);
            i = found + 1;
        }
        if (!fr.matches.isEmpty()) {
            fr.matchIndex = 0;
            for (int k = 0; k < fr.matches.size(); k++) {
                if (fr.matches.get(k) >= cursor) { fr.matchIndex = k; break; }
            }
        }
    }

    private void findNext() {
        if (fr.matches.isEmpty()) return;
        fr.matchIndex = (fr.matchIndex + 1) % fr.matches.size();
        jumpToMatch(fr.matchIndex);
    }

    private void findPrev() {
        if (fr.matches.isEmpty()) return;
        fr.matchIndex = (fr.matchIndex - 1 + fr.matches.size()) % fr.matches.size();
        jumpToMatch(fr.matchIndex);
    }

    private void jumpToMatch(int idx) {
        if (idx < 0 || idx >= fr.matches.size()) return;
        int offset = fr.matches.get(idx);
        int len    = fr.query.length();
        selAnchor  = offset;
        cursor     = offset + len;
        ensureCursorVisible();
    }

    private void replaceOne() {
        if (fr.matchIndex < 0 || fr.matchIndex >= fr.matches.size()) return;
        int offset = fr.matches.get(fr.matchIndex);
        int len    = fr.query.length();
        if (offset + len > text.length()) return;
        pushUndo();
        text.replace(offset, offset + len, fr.replace.toString());
        cursor    = offset + fr.replace.length();
        selAnchor = cursor;
        onTextChanged();
        updateFindMatches();
        if (!fr.matches.isEmpty()) {
            fr.matchIndex = Math.min(fr.matchIndex, fr.matches.size() - 1);
            jumpToMatch(fr.matchIndex);
        }
    }

    private void replaceAll() {
        if (fr.query.isEmpty()) return;
        updateFindMatches();
        if (fr.matches.isEmpty()) return;
        pushUndo();
        String src = text.toString();
        String q   = fr.query.toString().toLowerCase();
        String r   = fr.replace.toString();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < src.length()) {
            int found = src.toLowerCase().indexOf(q, i);
            if (found < 0) { sb.append(src, i, src.length()); break; }
            sb.append(src, i, found);
            sb.append(r);
            i = found + q.length();
        }
        text.setLength(0);
        text.append(sb);
        cursor = Math.min(cursor, text.length());
        selAnchor = cursor;
        onTextChanged();
        updateFindMatches();
    }

    private boolean handleFindBarKey(int key, int mods) {
        boolean shift = (mods & Keys.MOD_SHIFT)   != 0;
        boolean ctrl  = (mods & Keys.MOD_CONTROL) != 0;

        if (key == Keys.ESCAPE) { closeFindBar(); return true; }

        if (key == Keys.ENTER || key == Keys.KP_ENTER) {
            if (fr.focused == 1) { replaceOne(); return true; }
            if (shift) findPrev(); else findNext();
            return true;
        }

        if (key == Keys.TAB && fr.mode == 2) {
            fr.focused = 1 - fr.focused;
            return true;
        }

        if (ctrl && key == Keys.H) { replaceAll(); return true; }
        if (ctrl && key == Keys.F) { fr.focused = 0; fr.mode = Math.max(1, fr.mode); return true; }
        if (ctrl && key == Keys.R) { fr.mode = 2; fr.focused = 1; return true; }
        if (ctrl && key == Keys.C) { copyFindField(); return true; }
        if (ctrl && key == Keys.X) { cutFindField(); return true; }
        if (ctrl && key == Keys.V) { pasteFindField(); return true; }
        if (ctrl && key == Keys.A) { selectAllFindField(); return true; }

        StringBuilder active  = fr.focused == 0 ? fr.query    : fr.replace;
        int curVal             = fr.focused == 0 ? fr.cursor    : fr.replaceCursor;
        int anchorVal          = fr.focused == 0 ? fr.selAnchor : fr.replaceSelAnchor;
        boolean hasSel         = curVal != anchorVal;
        boolean changed        = false;

        if (key == Keys.BACKSPACE) {
            if (hasSel) {
                int lo = Math.min(curVal, anchorVal), hi = Math.max(curVal, anchorVal);
                active.delete(lo, hi); curVal = lo; anchorVal = lo; changed = true;
            } else if (curVal > 0) {
                active.deleteCharAt(curVal - 1); curVal--; anchorVal = curVal; changed = true;
            }
        } else if (key == Keys.DELETE) {
            if (hasSel) {
                int lo = Math.min(curVal, anchorVal), hi = Math.max(curVal, anchorVal);
                active.delete(lo, hi); curVal = lo; anchorVal = lo; changed = true;
            } else if (curVal < active.length()) {
                active.deleteCharAt(curVal); changed = true;
            }
        } else if (key == Keys.LEFT) {
            if (!shift && hasSel) { curVal = Math.min(curVal, anchorVal); anchorVal = curVal; }
            else { curVal = ctrl ? findWordLeft(active, curVal) : Math.max(0, curVal - 1); if (!shift) anchorVal = curVal; }
        } else if (key == Keys.RIGHT) {
            if (!shift && hasSel) { curVal = Math.max(curVal, anchorVal); anchorVal = curVal; }
            else { curVal = ctrl ? findWordRight(active, curVal) : Math.min(active.length(), curVal + 1); if (!shift) anchorVal = curVal; }
        } else if (key == Keys.HOME) {
            curVal = 0; if (!shift) anchorVal = 0;
        } else if (key == Keys.END) {
            curVal = active.length(); if (!shift) anchorVal = curVal;
        } else return false;

        if (fr.focused == 0) { fr.cursor = curVal; fr.selAnchor = anchorVal; }
        else                   { fr.replaceCursor = curVal; fr.replaceSelAnchor = anchorVal; }
        if (changed && fr.focused == 0) updateFindMatches();
        return true;
    }

    private int findFieldClickToCol(Font font, StringBuilder value, int relX, int cursorForScroll) {
        String s     = value.toString();
        int scrollOff = Math.max(0, font.width(s.substring(0, Math.min(cursorForScroll, s.length()))) - (FIND_BAR_W - 16) + 4);
        int targetX  = relX + scrollOff;
        int px = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = font.width(String.valueOf(s.charAt(i)));
            if (px + (cw + 1) / 2 >= targetX) return i;
            px += cw;
        }
        return s.length();
    }

    private int findWordLeft(StringBuilder s, int pos) {
        if (pos <= 0) return 0;
        pos--;
        while (pos > 0 && !Character.isLetterOrDigit(s.charAt(pos - 1)) && s.charAt(pos - 1) != '_') pos--;
        while (pos > 0 && (Character.isLetterOrDigit(s.charAt(pos - 1)) || s.charAt(pos - 1) == '_')) pos--;
        return pos;
    }

    private int findWordRight(StringBuilder s, int pos) {
        int len = s.length();
        while (pos < len && !Character.isLetterOrDigit(s.charAt(pos)) && s.charAt(pos) != '_') pos++;
        while (pos < len && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) pos++;
        return pos;
    }

    private void copyFindField() {
        StringBuilder active = fr.focused == 0 ? fr.query : fr.replace;
        int cur = fr.focused == 0 ? fr.cursor : fr.replaceCursor;
        int anc = fr.focused == 0 ? fr.selAnchor : fr.replaceSelAnchor;
        if (cur == anc) return;
        int lo = Math.min(cur, anc), hi = Math.max(cur, anc);
        Minecraft.getInstance().keyboardHandler.setClipboard(active.substring(lo, hi));
    }

    private void cutFindField() {
        copyFindField();
        StringBuilder active = fr.focused == 0 ? fr.query : fr.replace;
        int cur = fr.focused == 0 ? fr.cursor : fr.replaceCursor;
        int anc = fr.focused == 0 ? fr.selAnchor : fr.replaceSelAnchor;
        if (cur == anc) return;
        int lo = Math.min(cur, anc), hi = Math.max(cur, anc);
        active.delete(lo, hi);
        if (fr.focused == 0) { fr.cursor = lo; fr.selAnchor = lo; updateFindMatches(); }
        else { fr.replaceCursor = lo; fr.replaceSelAnchor = lo; }
    }

    private void pasteFindField() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        StringBuilder active = fr.focused == 0 ? fr.query : fr.replace;
        int cur = fr.focused == 0 ? fr.cursor : fr.replaceCursor;
        int anc = fr.focused == 0 ? fr.selAnchor : fr.replaceSelAnchor;
        if (cur != anc) {
            int lo = Math.min(cur, anc), hi = Math.max(cur, anc);
            active.delete(lo, hi); cur = lo;
        }
        active.insert(cur, clip); cur += clip.length();
        if (fr.focused == 0) { fr.cursor = cur; fr.selAnchor = cur; updateFindMatches(); }
        else { fr.replaceCursor = cur; fr.replaceSelAnchor = cur; }
    }

    private void selectAllFindField() {
        StringBuilder active = fr.focused == 0 ? fr.query : fr.replace;
        if (fr.focused == 0) { fr.selAnchor = 0; fr.cursor = active.length(); }
        else { fr.replaceSelAnchor = 0; fr.replaceCursor = active.length(); }
    }

    private boolean handleFindBarChar(char c) {
        if (c < 32 || c == 127) return false;
        StringBuilder active = fr.focused == 0 ? fr.query : fr.replace;
        int cur = fr.focused == 0 ? fr.cursor : fr.replaceCursor;
        int anc = fr.focused == 0 ? fr.selAnchor : fr.replaceSelAnchor;
        if (cur != anc) {
            int lo = Math.min(cur, anc), hi = Math.max(cur, anc);
            active.delete(lo, hi); cur = lo;
        }
        active.insert(cur, c); cur++;
        if (fr.focused == 0) { fr.cursor = cur; fr.selAnchor = cur; updateFindMatches(); }
        else { fr.replaceCursor = cur; fr.replaceSelAnchor = cur; }
        return true;
    }

    private void renderFindBar(Gfx g, Font font) {
        if (fr.mode == 0) return;
        UITheme t = NeoHouseConfig.get().uiTheme;
        int x0 = getX(), y0 = getY();
        int rows = fr.mode == 2 ? 2 : 1;
        int barH = 6 + rows * (FIND_FIELD_H + 4) + (fr.mode == 2 ? 16 : 0);

        int bx = x0 + width - FIND_BAR_W - 2;
        int by = y0 + 2;

        g.fill(bx, by, bx + FIND_BAR_W, by + barH, t.dialog(0xF2));
        g.fill(bx, by, bx + FIND_BAR_W, by + 1, t.accent());
        g.fill(bx, by, bx + 1, by + barH, t.accent());
        g.fill(bx + FIND_BAR_W - 1, by, bx + FIND_BAR_W, by + barH, t.accent());
        g.fill(bx, by + barH - 1, bx + FIND_BAR_W, by + barH, t.accent());

        String countLabel = fr.query.length() == 0 ? "" :
                fr.matches.isEmpty() ? "no matches" :
                (fr.matchIndex + 1) + "/" + fr.matches.size();
        int labelW = countLabel.isEmpty() ? 0 : font.width(countLabel) + 4;
        int fieldW = FIND_BAR_W - 12 - labelW;

        int fy = by + 4;
        renderFindField(g, font, t, bx + 4, fy, fieldW, fr.query, fr.cursor, fr.selAnchor, fr.focused == 0, "Find");
        if (!countLabel.isEmpty()) {
            int countColor = fr.matches.isEmpty() ? 0xFFFF6666 : 0xFF88CC88;
            g.drawString(font, countLabel, bx + FIND_BAR_W - labelW - 2, fy + 2, countColor, false);
        }

        if (fr.mode == 2) {
            int ry = fy + FIND_FIELD_H + 4;
            renderFindField(g, font, t, bx + 4, ry, fieldW, fr.replace, fr.replaceCursor, fr.replaceSelAnchor, fr.focused == 1, "Replace");

            int btnY = ry + FIND_FIELD_H + 3;
            int btn1X = bx + 4;
            int btn2X = btn1X + FIND_BTN_W + 3;
            renderFindButton(g, font, t, btn1X, btnY, FIND_BTN_W, "Replace");
            renderFindButton(g, font, t, btn2X, btnY, FIND_BTN_W + 10, "Replace All");
            fr.btnReplaceX    = btn1X; fr.btnReplaceY    = btnY;
            fr.btnReplaceAllX = btn2X; fr.btnReplaceAllY = btnY;
        } else {
            fr.btnReplaceX = -1; fr.btnReplaceAllX = -1;
        }
    }

    private void renderFindButton(Gfx g, Font font, UITheme t, int bx, int by, int bw, String label) {
        int bh = 11;
        g.fill(bx, by, bx + bw, by + bh, t.panel(0xFF));
        g.fill(bx, by, bx + bw, by + 1, t.accent());
        g.fill(bx, by, bx + 1, by + bh, t.accent());
        g.fill(bx + bw - 1, by, bx + bw, by + bh, t.accent());
        g.fill(bx, by + bh - 1, bx + bw, by + bh, t.accent());
        int lw = font.width(label);
        g.drawString(font, label, bx + (bw - lw) / 2, by + 2, 0xFFCCCCCC, false);
    }

    private void renderFindField(Gfx g, Font font, UITheme t,
                                  int fx, int fy, int fieldW,
                                  StringBuilder value, int cur, int anchor, boolean focused, String placeholder) {
        g.fill(fx, fy, fx + fieldW, fy + FIND_FIELD_H, focused ? 0xFF2A2A3A : 0xFF222222);
        g.fill(fx, fy, fx + fieldW, fy + 1, focused ? t.accent() : t.divider());

        String display = value.toString();
        if (display.isEmpty()) {
            g.drawString(font, placeholder, fx + 2, fy + 2, 0xFF555555, false);
            return;
        }

        int maxW    = fieldW - 4;
        int curPx   = font.width(display.substring(0, Math.min(cur, display.length())));
        int scrollOff = Math.max(0, curPx - maxW + 4);

        g.enableScissor(fx, fy, fx + fieldW, fy + FIND_FIELD_H);

        if (focused && cur != anchor) {
            int lo = Math.min(cur, anchor), hi = Math.max(cur, anchor);
            int sx = fx + 2 + font.width(display.substring(0, Math.min(lo, display.length()))) - scrollOff;
            int ex = fx + 2 + font.width(display.substring(0, Math.min(hi, display.length()))) - scrollOff;
            g.fill(sx, fy + 1, ex, fy + FIND_FIELD_H - 1, 0x664477AA);
        }

        g.drawString(font, display, fx + 2 - scrollOff, fy + 2, 0xFFDDDDDD, false);

        if (focused && (System.currentTimeMillis() / 530) % 2 == 0) {
            int cx = fx + 2 + curPx - scrollOff;
            g.fill(cx, fy + 1, cx + 1, fy + FIND_FIELD_H - 1, 0xFFCCCCCC);
        }
        g.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {}
}
