package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.SyntaxTheme;
import com.drakou111.neohouse.UITheme;
import com.drakou111.neohouse.compiler.enums.Param;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.AddCondition;
import com.drakou111.neohouse.constants.Event;
import com.drakou111.neohouse.constants.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxEditorWidget extends AbstractWidget {

    // ── Text state ────────────────────────────────────────────────────────────
    private final StringBuilder text = new StringBuilder();
    private int cursor    = 0;
    private int selAnchor = 0;
    private int scrollLine = 0;
    private int scrollX    = 0;

    private long lastClickTime = 0;
    private int  lastClickPos  = -1;

    // ── Error state ───────────────────────────────────────────────────────────
    private int    errorLine     = -1;
    private int    errorColStart = -1;
    private int    errorTokenLen = 0;
    private String errorMsg      = null;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Runnable onChange;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LINE_H          = 10;
    private static final int GUTTER_W        = 30;
    private static final int PAD_X           = 4;
    private static final int PAD_Y           = 2;
    private static final int ERROR_BAR_H_MIN = 14;
    private static final int SPACES_PER_TAB  = 4;
    private static final int SB_W            = 4;
    private static final int SB_PAD          = 2;

    // ── Vertical scrollbar drag state ─────────────────────────────────────────
    private boolean sbDragging           = false;
    private int     sbDragStartY         = 0;
    private int     sbDragStartScrollLine = 0;

    // ── Highlight spans ───────────────────────────────────────────────────────
    private record Span(int start, int end, int color) {}
    private final List<Span> spans = new ArrayList<>();

    // ── Inline hints ──────────────────────────────────────────────────────────
    /** charOffset is position within the line string where the hint is injected. */
    private record InlineHint(int charOffset, String displayText, boolean isActive) {}

    // ── Tokenizer helper (for 2-pass span coloring) ───────────────────────────
    private record Tok(TokenType type, String text, int pos) {}

    // ── Undo / Redo ───────────────────────────────────────────────────────────
    private record UndoEntry(String text, int cursor) {}
    private final List<UndoEntry> undoStack = new ArrayList<>();
    private final List<UndoEntry> redoStack = new ArrayList<>();
    private static final int UNDO_LIMIT = 200;

    // ── Enum suggestions ──────────────────────────────────────────────────────
    private String[] activeSuggestions     = null;
    private int      suggestionIndex       = 0;
    private int      suggestionScrollOffset = 0;
    private int      suggestionTypedLen    = 0;
    private boolean  suggestionIsString    = false;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    // Cached panel bounds for mouse-over detection in mouseScrolled
    private int suggPanelX = -1, suggPanelY = -1, suggPanelW = 0, suggPanelH = 0;

    // ── Parameter hints (cursor tracking) ────────────────────────────────────
    private int    paramHintIndex = -1;
    private String paramHintFunc  = null;
    private static final Pattern USER_FUNC_PAT =
            Pattern.compile("\\bfunction\\b\\s+(?:\\w+\\s+)?(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern PH_SUGGEST_PAT = Pattern.compile("ph\\.(\\w*)$");

    // Inline-hint labels for placeholder parameters (names not stored in the enum)
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
                .replace("\t", " ".repeat(SPACES_PER_TAB));
        text.append(normalized);
        cursor    = 0;
        selAnchor = 0;
        scrollLine = 0;
        scrollX    = 0;
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

    // ── Error bar height (dynamic word-wrap) ──────────────────────────────────

    private int errorBarH() {
        if (errorMsg == null) return 0;
        Font font = Minecraft.getInstance().font;
        List<String> wrapped = wrapErrorMsg(font, width - 8);
        return Math.max(ERROR_BAR_H_MIN, wrapped.size() * 10 + 4);
    }

    private List<String> wrapErrorMsg(Font font, int maxW) {
        String line = firstMeaningfulLine(errorMsg);
        List<String> result = new ArrayList<>();
        String[] words = line.split(" ", -1);
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
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
        if (current.length() > 0) result.add(current.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
        Font font  = Minecraft.getInstance().font;
        int  x0    = getX(), y0 = getY();
        int  errorH = errorBarH();
        int  codeH  = height - errorH;
        UITheme t = UITheme.byId(NeoHouseConfig.get().uiTheme);

        g.fill(x0,             y0, x0 + width,        y0 + codeH, 0xE01E1E1E);
        g.fill(x0,             y0, x0 + GUTTER_W,     y0 + codeH, 0xE0252526);
        g.fill(x0 + GUTTER_W, y0, x0 + GUTTER_W + 1, y0 + codeH, t.accent());

        List<String> lines    = getLines();
        int          visLines = Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
        scrollLine = clampScrollLine(scrollLine, lines.size(), visLines);
        int[] lineOffsets = buildLineOffsets(lines);

        int[] curLC  = offsetToLC(cursor, lines);
        boolean hasSel = cursor != selAnchor;

        int codeAreaRight = x0 + width - SB_W - SB_PAD * 2;
        int endLine = Math.min(scrollLine + visLines, lines.size());

        g.enableScissor(x0 + GUTTER_W + 1, y0, codeAreaRight, y0 + codeH);

        for (int li = scrollLine; li < endLine; li++) {
            int    ry        = y0 + PAD_Y + (li - scrollLine) * LINE_H;
            String line      = lines.get(li);
            int    lineStart = lineOffsets[li];

            // Compute inline hints for this line
            String activeFunc = (isFocused() && li == curLC[0]) ? paramHintFunc : null;
            int    activeIdx  = (isFocused() && li == curLC[0]) ? paramHintIndex : -1;
            List<InlineHint> lineHints = computeLineHints(lines, li, activeFunc, activeIdx);

            if (li == errorLine) {
                g.fill(x0 + GUTTER_W + 1, ry - 1, x0 + width, ry + LINE_H - 1, 0x44FF3333);
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

            if (li == errorLine && errorColStart >= 0 && errorTokenLen > 0) {
                int uLo = Math.min(errorColStart, line.length());
                int uHi = Math.min(errorColStart + errorTokenLen, line.length());
                if (uHi > uLo) {
                    int ux0 = codeX() + lineColToPixelX(font, line, uLo, lineHints) - scrollX;
                    int ux1 = codeX() + lineColToPixelX(font, line, uHi, lineHints) - scrollX;
                    int uy  = ry + LINE_H - 2;
                    for (int wx = ux0; wx < ux1; wx++) {
                        g.fill(wx, uy + (wx % 2), wx + 1, uy + (wx % 2) + 1, 0xFFFF4444);
                    }
                }
            }

            if (isFocused() && li == curLC[0]) {
                int col = Math.min(curLC[1], line.length());
                int cx  = codeX() + lineColToPixelX(font, line, col, lineHints) - scrollX;
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
                    li == errorLine ? 0xFFFF6666 : 0xFF636363, false);
        }

        renderScrollbar(g, x0, y0, codeH, lines.size(), visLines, t);

        if (errorMsg != null) {
            int ey = y0 + codeH;
            g.fill(x0, ey,     x0 + width, ey + errorH, 0xEE3B1111);
            g.fill(x0, ey,     x0 + width, ey + 1,      0xFFAA3333);
            List<String> wrapped = wrapErrorMsg(font, width - 8);
            for (int wi = 0; wi < wrapped.size(); wi++) {
                g.drawString(font, wrapped.get(wi), x0 + 4, ey + 3 + wi * 10, 0xFFFF8888, false);
            }
        }

        int border = t.accent();
        g.fill(x0,             y0,              x0 + width,     y0 + 1,         border);
        g.fill(x0,             y0 + height - 1, x0 + width,     y0 + height,    border);
        g.fill(x0,             y0,              x0 + 1,         y0 + height,    border);
        g.fill(x0 + width - 1, y0,              x0 + width,     y0 + height,    border);

        renderSuggestions(g, font, x0, y0, lines);
    }

    private void renderScrollbar(GuiGraphics g, int x0, int y0, int codeH, int totalLines, int visLines, UITheme t) {
        if (totalLines <= visLines) return;
        int trackX = x0 + width - SB_W - SB_PAD;
        int trackY = y0;
        int trackH = codeH;
        g.fill(trackX, trackY, trackX + SB_W, trackY + trackH, 0x33FFFFFF);
        int maxOff  = totalLines - visLines;
        int thumbH  = Math.max(8, trackH * visLines / totalLines);
        int thumbY  = trackY + (maxOff > 0 ? (trackH - thumbH) * scrollLine / maxOff : 0);
        int color   = sbDragging ? 0xCCDDDDDD : (0x99000000 | (t.muted() & 0xFFFFFF));
        g.fill(trackX, thumbY, trackX + SB_W, thumbY + thumbH, color);
    }

    private int codeX() { return getX() + GUTTER_W + 1 + PAD_X; }

    private static String firstMeaningfulLine(String s) {
        for (String part : s.split("\n")) {
            String t = part.trim();
            if (!t.isEmpty()) return t;
        }
        return s.trim();
    }

    // ── Inline hint computation ───────────────────────────────────────────────

    private List<InlineHint> computeLineHints(List<String> allLines, int lineIndex, String activeFuncName, int activeParamIdx) {
        if (!NeoHouseConfig.get().showArgumentHints) return List.of();

        String line = allLines.get(lineIndex);
        List<InlineHint> hints = new ArrayList<>();

        // Find the '(' that belongs to a function DEFINITION so we can skip it.
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

        // Merge with continuation hints (args from calls that started on a previous line)
        List<InlineHint> contHints = computeContinuationHints(allLines, lineIndex, activeFuncName, activeParamIdx);
        if (!contHints.isEmpty()) {
            hints.addAll(contHints);
            hints.sort(Comparator.comparingInt(InlineHint::charOffset));
        }

        return hints;
    }

    /** For a line that is a continuation of a multi-line function call started on a previous line,
     *  returns inline hints for the argument positions that appear on this line. */
    private List<InlineHint> computeContinuationHints(List<String> allLines, int lineIndex, String activeFuncName, int activeParamIdx) {
        int[] offs = buildLineOffsets(allLines);
        int lineStart = offs[lineIndex];
        if (lineStart == 0) return List.of();

        String fullText = text.toString();

        // Backward scan to find the enclosing unclosed '('
        int depth = 0;
        int funcParenPos = -1;
        for (int i = lineStart - 1; i >= 0; i--) {
            char c = fullText.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && fullText.charAt(i) != '"') i--;
            } else if (c == ')') {
                depth++;
            } else if (c == '(' && depth > 0) {
                depth--;
            } else if (c == '(') {
                funcParenPos = i;
                break;
            }
        }
        if (funcParenPos < 0) return List.of();

        // Extract the function name immediately before the '('
        int nameEnd = funcParenPos;
        while (nameEnd > 0 && fullText.charAt(nameEnd - 1) == ' ') nameEnd--;
        int nameStart = nameEnd;
        while (nameStart > 0 && (Character.isLetterOrDigit(fullText.charAt(nameStart - 1)) || fullText.charAt(nameStart - 1) == '_'))
            nameStart--;
        if (nameStart >= nameEnd) return List.of();
        String funcName = fullText.substring(nameStart, nameEnd);

        List<String> paramLabels = resolveParamNames(funcName);
        if (paramLabels == null || paramLabels.isEmpty()) return List.of();

        // Forward scan from '(' to lineStart to determine current argIndex and atArgStart state
        int argIndex = 0;
        int scanDepth = 0;
        boolean atArgStart = true; // immediately after '(' is a potential arg start

        for (int i = funcParenPos + 1; i < lineStart; i++) {
            char c = fullText.charAt(i);
            if (c == '"') {
                atArgStart = false;
                i++;
                while (i < lineStart && fullText.charAt(i) != '"') i++;
            } else if (c == '(' || c == '[' || c == '{') {
                scanDepth++;
                atArgStart = false;
            } else if (c == ')' || c == ']' || c == '}') {
                if (scanDepth == 0) return List.of(); // call ended before this line
                scanDepth--;
                atArgStart = false;
            } else if (c == ',' && scanDepth == 0) {
                argIndex++;
                atArgStart = true;
            } else if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                atArgStart = false;
            }
        }

        // Now scan the current line to add hints
        List<InlineHint> hints = new ArrayList<>();
        String curLine = allLines.get(lineIndex);
        int col = 0;

        // Skip leading whitespace
        while (col < curLine.length() && curLine.charAt(col) == ' ') col++;

        // If we're at the start of an arg on this line, emit a hint
        if (atArgStart && col < curLine.length() && curLine.charAt(col) != ')' && argIndex < paramLabels.size()) {
            String label = paramLabels.get(argIndex);
            int colon = label.indexOf(':');
            String name = (colon >= 0 ? label.substring(0, colon) : label).trim();
            boolean isActive = funcName.equals(activeFuncName) && argIndex == activeParamIdx;
            hints.add(new InlineHint(col, name + ": ", isActive));
        }

        // Continue scanning for additional comma-separated args on this line
        while (col < curLine.length()) {
            char c = curLine.charAt(col);
            if (c == '"') {
                col++;
                while (col < curLine.length() && curLine.charAt(col) != '"') col++;
                if (col < curLine.length()) col++;
            } else if (c == '(' || c == '[' || c == '{') {
                scanDepth++;
                col++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (scanDepth == 0) break;
                scanDepth--;
                col++;
            } else if (c == ',' && scanDepth == 0) {
                argIndex++;
                col++;
                while (col < curLine.length() && curLine.charAt(col) == ' ') col++;
                if (col < curLine.length() && curLine.charAt(col) != ')' && argIndex < paramLabels.size()) {
                    String label = paramLabels.get(argIndex);
                    int colon = label.indexOf(':');
                    String name = (colon >= 0 ? label.substring(0, colon) : label).trim();
                    boolean isActive = funcName.equals(activeFuncName) && argIndex == activeParamIdx;
                    hints.add(new InlineHint(col, name + ": ", isActive));
                }
            } else {
                col++;
            }
        }

        return hints;
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

    /**
     * Returns pixel width from the left edge of the line to character column {@code col},
     * accounting for any inline hints injected before that column.
     */
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

    /**
     * Maps a pixel X offset (relative to codeX()) on a line back to a character column,
     * accounting for inline hints.
     */
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

    private void renderHighlightedLine(GuiGraphics g, Font font,
                                       String line, int lineStart,
                                       int x, int y, List<InlineHint> hints) {
        int lineEnd   = lineStart + line.length();
        int curX      = x;
        int charDrawn = 0;
        int hintPtr   = 0;

        for (Span sp : spans) {
            if (sp.end() <= lineStart) continue;
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

    /**
     * Draws line[from..to) with the given color, injecting any inline hints whose
     * charOffset falls in [from, to).
     * Returns {newCurX, to, newHintPtr}.
     */
    private int[] drawSegment(GuiGraphics g, Font font, String line,
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
            UITheme hintTheme = UITheme.byId(NeoHouseConfig.get().uiTheme);
            int    htColor = hint.isActive() ? hintTheme.muted() : hintTheme.divider();
            int    hw      = font.width(ht);
            g.drawString(font, ht, curX, y, htColor, false);
            curX += hw;
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

    // Active theme, set at the start of each rebuildSpans call
    private SyntaxTheme activeTheme = SyntaxTheme.VSCODE_DARK;

    private void rebuildSpans() {
        spans.clear();
        activeTheme = SyntaxTheme.byId(NeoHouseConfig.get().syntaxTheme);
        String src = text.toString();
        int pos = 0;
        List<Tok> toks = new ArrayList<>();
        while (pos < src.length()) {
            String  rest    = src.substring(pos);
            boolean matched = false;
            for (TokenType tt : TokenType.values()) {
                if (tt.pattern == null) continue;
                Matcher m = tt.pattern.matcher(rest);
                if (!m.lookingAt()) continue;
                String s = m.group();
                toks.add(new Tok(tt, s, pos));
                pos += s.length();
                matched = true;
                break;
            }
            if (!matched) pos++;
        }
        for (int i = 0; i < toks.size(); i++) {
            Tok tok = toks.get(i);
            int color = contextualColor(toks, i);
            spans.add(new Span(tok.pos(), tok.pos() + tok.text().length(), color));
        }
    }

    private int contextualColor(List<Tok> toks, int i) {
        SyntaxTheme th = activeTheme;
        Tok tok = toks.get(i);
        if (tok.type() == TokenType.WHITESPACE) return 0;
        if (tok.type() != TokenType.IDENT) return baseTokenColor(tok.type(), tok.text());

        Tok next   = nextNonWsTok(toks, i + 1);
        int prevI  = prevNonWsIdx(toks, i - 1);
        Tok prev   = prevI >= 0 ? toks.get(prevI) : null;

        // Scope prefix letters (color the letter itself)
        if ("p".equals(tok.text())  && next != null && next.type() == TokenType.DOT)    return th.playerVar;
        if ("g".equals(tok.text())  && next != null && next.type() == TokenType.DOT)    return th.globalVar;
        if ("t".equals(tok.text())  && next != null && next.type() == TokenType.LBRACK) return th.teamVar;
        if ("ph".equals(tok.text()) && next != null && next.type() == TokenType.DOT)    return th.phVar;

        // Identifier that follows a scope dot (p.name, g.name, ph.name)
        if (prev != null && prev.type() == TokenType.DOT) {
            int prevPrevI = prevNonWsIdx(toks, prevI - 1);
            Tok prevPrev  = prevPrevI >= 0 ? toks.get(prevPrevI) : null;

            if (prevPrev != null && "ph".equals(prevPrev.text())) {
                return Placeholder.searchByName(tok.text()) != null ? th.phNameValid : th.plain;
            }
            if (prevPrev != null && "p".equals(prevPrev.text())) return th.playerVar;
            if (prevPrev != null && "g".equals(prevPrev.text())) return th.globalVar;

            // t[...].fieldName — walk back to find matching '[' and verify 't' precedes it
            if (prevPrev != null && prevPrev.type() == TokenType.RBRACK) {
                int depth = 1;
                int k = prevPrevI - 1;
                while (k >= 0 && depth > 0) {
                    TokenType tt = toks.get(k).type();
                    if (tt == TokenType.RBRACK)      depth++;
                    else if (tt == TokenType.LBRACK) depth--;
                    k--;
                }
                // k is now one before the LBRACK; find the non-ws token before the bracket
                Tok beforeBrack = prevNonWsTok(toks, k);
                if (beforeBrack != null && "t".equals(beforeBrack.text())) return th.teamVar;
            }
        }

        return baseTokenColor(tok.type(), tok.text());
    }

    private int baseTokenColor(TokenType t, String text) {
        SyntaxTheme th = activeTheme;
        return switch (t) {
            case FUNCTION, RETURN, CANCEL, LOOP, GLOBAL,
                 IF, ELSE, REPEAT, EVENT, RANDOM, COMMAND, IMPORT, MENU -> th.keyword;
            case LONG, DOUBLE, STRING, BOOL, VAR          -> th.type;
            case TRUE, FALSE                               -> th.keyword;
            case LONG_CAST, DOUBLE_CAST, STRING_CAST       -> th.type;
            case LONG_VALUE, DOUBLE_VALUE                  -> th.number;
            case STRING_VALUE                              -> th.string;
            case COMMENT_LINE, COMMENT_BLOCK               -> th.comment;
            case NOTE                                      -> th.note;
            case CONST_IDENT                               -> th.phVar;
            case IDENT -> AddAction.searchByName(text) != null ? th.action : th.plain;
            case WHITESPACE                                -> 0;
            default                                        -> th.plain;
        };
    }

    private static Tok nextNonWsTok(List<Tok> toks, int from) {
        for (int j = from; j < toks.size(); j++)
            if (toks.get(j).type() != TokenType.WHITESPACE) return toks.get(j);
        return null;
    }

    private static Tok prevNonWsTok(List<Tok> toks, int from) {
        for (int j = from; j >= 0; j--)
            if (toks.get(j).type() != TokenType.WHITESPACE) return toks.get(j);
        return null;
    }

    private static int prevNonWsIdx(List<Tok> toks, int from) {
        for (int j = from; j >= 0; j--)
            if (toks.get(j).type() != TokenType.WHITESPACE) return j;
        return -1;
    }

    // ── Input handling ────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl  = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT)   != 0;

        if (!ctrl && !shift && activeSuggestions != null && activeSuggestions.length > 0) {
            if (key == GLFW.GLFW_KEY_TAB)    { applySuggestion(); return true; }
            if (key == GLFW.GLFW_KEY_UP)     { suggestionIndex = (suggestionIndex - 1 + activeSuggestions.length) % activeSuggestions.length; clampSuggestionScroll(); return true; }
            if (key == GLFW.GLFW_KEY_DOWN)   { suggestionIndex = (suggestionIndex + 1) % activeSuggestions.length; clampSuggestionScroll(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { dismissSuggestions(); return true; }
        }

        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> { selectAll();           return true; }
                case GLFW.GLFW_KEY_C -> { copySelection();       return true; }
                case GLFW.GLFW_KEY_X -> { cutSelection();        return true; }
                case GLFW.GLFW_KEY_V -> { pasteClipboard();      return true; }
                case GLFW.GLFW_KEY_Z -> { if (shift) redo(); else undo(); return true; }
                case GLFW.GLFW_KEY_Y -> { redo();                return true; }
                case GLFW.GLFW_KEY_HOME  -> { moveCursor(0,             shift); return true; }
                case GLFW.GLFW_KEY_END   -> { moveCursor(text.length(), shift); return true; }
                case GLFW.GLFW_KEY_LEFT  -> { moveWordLeft(shift);       return true; }
                case GLFW.GLFW_KEY_RIGHT -> { moveWordRight(shift);      return true; }
            }
        }

        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                if (hasSelection()) {
                    if (shift) dedentSelection(); else indentSelection();
                } else {
                    if (shift) dedent(); else insertString(" ".repeat(SPACES_PER_TAB));
                }
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

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isMouseOver(mx, my)) { setFocused(false); activeSuggestions = null; return false; }

        // Vertical scrollbar click
        int x0 = getX(), y0 = getY();
        int errorH = errorBarH();
        int codeH  = height - errorH;
        List<String> lines = getLines();
        int visLines = Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
        if (lines.size() > visLines) {
            int trackX = x0 + width - SB_W - SB_PAD;
            int trackH = codeH;
            if (mx >= trackX - 2 && mx <= trackX + SB_W + 2 && my >= y0 && my < y0 + trackH) {
                int maxOff = lines.size() - visLines;
                int thumbH = Math.max(8, trackH * visLines / lines.size());
                int thumbY = y0 + (maxOff > 0 ? (trackH - thumbH) * scrollLine / maxOff : 0);
                if (my >= thumbY && my < thumbY + thumbH) {
                    sbDragging            = true;
                    sbDragStartY          = (int) my;
                    sbDragStartScrollLine = scrollLine;
                } else {
                    int newScroll = (int) ((my - y0 - thumbH / 2.0) * maxOff / Math.max(1, trackH - thumbH));
                    scrollLine = Math.max(0, Math.min(maxOff, newScroll));
                }
                return true;
            }
        }

        setFocused(true);
        int pos = hitToOffset((int) mx, (int) my);
        if (pos >= 0) {
            long now = System.currentTimeMillis();
            boolean isDoubleClick = (now - lastClickTime < 500) && (pos == lastClickPos);
            lastClickTime = now;
            lastClickPos  = pos;

            if (isDoubleClick) {
                selectWordAt(pos);
            } else {
                cursor    = pos;
                selAnchor = pos;
            }
        }
        updateSuggestions();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sbDragging && btn == 0) {
            int errorH = errorBarH();
            int codeH  = height - errorH;
            List<String> lines = getLines();
            int visLines = Math.max(1, (codeH - PAD_Y * 2) / LINE_H);
            int trackH   = codeH;
            int thumbH   = Math.max(8, trackH * visLines / lines.size());
            int maxOff   = Math.max(0, lines.size() - visLines);
            int slideH   = trackH - thumbH;
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
    public boolean mouseReleased(double mx, double my, int btn) {
        if (sbDragging && btn == 0) { sbDragging = false; return true; }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!isMouseOver(mx, my)) return false;

        if (activeSuggestions != null && activeSuggestions.length > MAX_VISIBLE_SUGGESTIONS
                && suggPanelX >= 0
                && mx >= suggPanelX && mx < suggPanelX + suggPanelW
                && my >= suggPanelY && my < suggPanelY + suggPanelH) {
            int maxOff = activeSuggestions.length - MAX_VISIBLE_SUGGESTIONS;
            suggestionScrollOffset = Math.max(0, Math.min(maxOff, suggestionScrollOffset - (int) Math.signum(dy)));
            return true;
        }

        if (Screen.hasShiftDown()) {
            scrollX = Math.max(0, scrollX - (int) Math.signum(dy) * 20);
            return true;
        }
        scrollLine = clampScrollLine(scrollLine - (int) Math.signum(dy),
                getLines().size(), visibleLines());
        return true;
    }

    // ── Text editing ──────────────────────────────────────────────────────────

    private void insertChar(char c) {
        // Capture auto-indent info before mutating the text
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

    private void indentSelection() {
        List<String> lines = getLines();
        int[] offs = buildLineOffsets(lines);
        int selLo = Math.min(cursor, selAnchor);
        int selHi = Math.max(cursor, selAnchor);

        int firstLine = offsetToLC(selLo, lines)[0];
        int[] hiLC = offsetToLC(selHi, lines);
        int lastLine = (hiLC[1] == 0 && hiLC[0] > firstLine) ? hiLC[0] - 1 : hiLC[0];

        String indent = " ".repeat(SPACES_PER_TAB);
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
            if (offs[li] < pos) shift += SPACES_PER_TAB;
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
            for (int j = 0; j < Math.min(SPACES_PER_TAB, lineStr.length()); j++) {
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
        updateSuggestions();
        if (onChange != null) onChange.run();
    }

    // ── Cursor movement ───────────────────────────────────────────────────────

    private void moveCursor(int pos, boolean select) {
        pos    = Math.max(0, Math.min(pos, text.length()));
        cursor = pos;
        if (!select) selAnchor = pos;
        ensureCursorVisible();
        updateSuggestions();
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

    private void moveWordLeft(boolean select) {
        int pos = cursor;
        if (pos <= 0) { moveCursor(0, select); return; }
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
        moveCursor(pos, select);
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
            // Select the entire current line (including trailing newline) and copy it
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
        if (clip == null || clip.isEmpty()) return;
        clip = clip.replace("\r\n", "\n").replace("\r", "\n")
                   .replace("\t", " ".repeat(SPACES_PER_TAB));
        insertString(clip);
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

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

    // ── Scroll management ─────────────────────────────────────────────────────

    private void ensureCursorVisible() {
        List<String> lines = getLines();
        int[] lc  = offsetToLC(cursor, lines);
        int   vis = visibleLines();
        if (lc[0] < scrollLine)        scrollLine = lc[0];
        if (lc[0] >= scrollLine + vis) scrollLine = lc[0] - vis + 1;
        scrollLine = clampScrollLine(scrollLine, lines.size(), vis);

        // Horizontal scroll
        Font font = Minecraft.getInstance().font;
        String line = lines.get(lc[0]);
        List<InlineHint> hints = computeLineHints(lines, lc[0], null, -1);
        int col      = Math.min(lc[1], line.length());
        int cursorPx = lineColToPixelX(font, line, col, hints);
        int viewW    = width - GUTTER_W - 1 - PAD_X * 2 - SB_W - SB_PAD * 2;
        if (viewW < 20) viewW = 20;
        if (cursorPx < scrollX)              scrollX = cursorPx;
        if (cursorPx > scrollX + viewW - 4)  scrollX = cursorPx - viewW + 4;
        if (scrollX < 0) scrollX = 0;
    }

    private int clampScrollLine(int s, int lineCount, int vis) {
        return Math.max(0, Math.min(s, Math.max(0, lineCount - vis)));
    }

    private int visibleLines() {
        int codeH = height - errorBarH();
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

    // ── Utility ───────────────────────────────────────────────────────────────

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

    // ── Suggestions ───────────────────────────────────────────────────────────

    public boolean hasSuggestions() {
        return activeSuggestions != null && activeSuggestions.length > 0;
    }

    public void dismissSuggestions() {
        activeSuggestions      = null;
        suggestionIndex        = 0;
        suggestionScrollOffset = 0;
        suggPanelX             = -1;
    }

    private void updateSuggestions() {
        String[] prev    = activeSuggestions;
        int      prevIdx = suggestionIndex;
        activeSuggestions = computeSuggestionsAtCursor();
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

        // ── Event name context: "event <partial>" ─────────────────────────────
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

        // ── Placeholder name context: "ph.<partial>" ──────────────────────────
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

        // ── Enum suggestions inside string literals ────────────────────────────
        int quoteCount = 0;
        for (int i = 0; i < beforeCursor.length(); i++) if (beforeCursor.charAt(i) == '"') quoteCount++;
        if (quoteCount % 2 == 0) return null;

        int quotePos   = beforeCursor.lastIndexOf('"');
        String typedIn = beforeCursor.substring(quotePos + 1);
        String beforeQ = beforeCursor.substring(0, quotePos);

        int paramIndex = 0, depth = 0, funcNameEnd = -1;
        for (int i = beforeQ.length() - 1; i >= 0; i--) {
            char c = beforeQ.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && beforeQ.charAt(i) != '"') i--;
                continue;
            }
            if      (c == ')')               depth++;
            else if (c == '(' && depth > 0)  depth--;
            else if (c == '(' )              { funcNameEnd = i; break; }
            else if (c == ',' && depth == 0) paramIndex++;
        }
        if (funcNameEnd < 0) return null;

        String beforeParen = beforeQ.substring(0, funcNameEnd).stripTrailing();
        int nameStart = beforeParen.length();
        while (nameStart > 0 && Character.isLetterOrDigit(beforeParen.charAt(nameStart - 1))) nameStart--;
        String funcName = beforeParen.substring(nameStart);
        if (funcName.isEmpty()) return null;

        String[] allowed = null;
        AddAction action = AddAction.searchByName(funcName);
        if (action != null && paramIndex < action.params.size()) {
            allowed = action.params.get(paramIndex).enumValues();
        }
        if (allowed == null) {
            AddCondition cond = AddCondition.searchByName(funcName);
            if (cond != null && paramIndex < cond.params.size()) {
                allowed = cond.params.get(paramIndex).enumValues();
            }
        }
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

        int paramIndex = 0, depth = 0, funcNameEnd = -1;
        for (int i = beforeCursor.length() - 1; i >= 0; i--) {
            char c = beforeCursor.charAt(i);
            if (c == '"') {
                i--;
                while (i >= 0 && beforeCursor.charAt(i) != '"') i--;
                continue;
            }
            if      (c == ')')              depth++;
            else if (c == '(' && depth > 0) depth--;
            else if (c == '(')             { funcNameEnd = i; break; }
            else if (c == ',' && depth == 0) paramIndex++;
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

    private void renderSuggestions(GuiGraphics g, Font font, int x0, int y0, List<String> lines) {
        UITheme t = UITheme.byId(NeoHouseConfig.get().uiTheme);
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

        int px = Math.min(cx, x0 + width - panelW - 1);
        int py = (cy + panelH > y0 + height) ? cy - panelH - LINE_H : cy;

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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}
