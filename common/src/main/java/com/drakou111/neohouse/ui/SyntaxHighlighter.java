package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.config.SyntaxTheme;

import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SyntaxHighlighter {

    record Span(int start, int end, int color) {}

    private record Tok(TokenType type, String text, int pos) {}

    private SyntaxTheme activeTheme = SyntaxTheme.VSCODE_DARK;

    // Minecraft chat color codes (&0–&f), in index order 0–15.
    private static final int[] MC_COLORS = {
        0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
        0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
        0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
        0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF,
    };

    /** Returns the Minecraft display color for a hex digit code, or -1 if not a color code. */
    private static int mcColorFor(char code) {
        code = Character.toLowerCase(code);
        if (code >= '0' && code <= '9') return MC_COLORS[code - '0'];
        if (code >= 'a' && code <= 'f') return MC_COLORS[10 + (code - 'a')];
        return -1;
    }

    /** True for any valid Minecraft formatting code character (&0–&f, &k–&o, &r). */
    private static boolean isMcCode(char code) {
        code = Character.toLowerCase(code);
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || "klmnor".indexOf(code) >= 0;
    }

    /** Dims a color by multiplying each channel by 2/3. */
    private static int dimColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b =  color        & 0xFF;
        return 0xFF000000 | ((r * 2 / 3) << 16) | ((g * 2 / 3) << 8) | (b * 2 / 3);
    }

    public List<Span> buildSpans(String src) {
        List<Span> spans = new ArrayList<>();
        activeTheme = NeoHouseConfig.get().syntaxTheme;

        int len = src.length();

        // Build one reusable matcher per token type over the WHOLE source. Advancing the region
        // start (instead of substringing the tail) keeps this linear — the previous
        // src.substring(pos) per position made highlighting O(n^2), which froze large files.
        List<TokenType> types = new ArrayList<>();
        List<Matcher>   matchers = new ArrayList<>();
        for (TokenType tt : TokenType.values()) {
            if (tt.pattern == null) continue;
            types.add(tt);
            matchers.add(tt.pattern.matcher(src));
        }

        int pos = 0;
        List<Tok> toks = new ArrayList<>();
        while (pos < len) {
            boolean matched = false;
            for (int t = 0; t < matchers.size(); t++) {
                Matcher m = matchers.get(t);
                m.region(pos, len);
                if (!m.lookingAt()) continue;
                String s = m.group();
                if (s.isEmpty()) continue; // avoid an infinite loop on a zero-width match
                toks.add(new Tok(types.get(t), s, pos));
                pos += s.length();
                matched = true;
                break;
            }
            if (!matched) pos++;
        }

        for (int i = 0; i < toks.size(); i++) {
            Tok tok = toks.get(i);
            if (tok.type() == TokenType.STRING_VALUE) {
                addStringSpans(spans, tok.text(), tok.pos());
            } else {
                int color = contextualColor(toks, i);
                spans.add(new Span(tok.pos(), tok.pos() + tok.text().length(), color));
            }
        }

        return spans;
    }

    private void addStringSpans(List<Span> spans, String raw, int basePos) {
        int len = raw.length();
        if (len == 0) return;

        spans.add(new Span(basePos, basePos + 1, activeTheme.string));

        int contentEnd = (len >= 2 && raw.charAt(len - 1) == '"') ? len - 1 : len;
        String content = raw.substring(1, contentEnd);

        int currentColor = activeTheme.string;
        int segStart     = 0;

        for (int i = 0; i < content.length(); ) {
            if (content.charAt(i) == '&' && i + 1 < content.length()) {
                char code = content.charAt(i + 1);
                if (isMcCode(code)) {
                    if (i > segStart)
                        spans.add(new Span(basePos + 1 + segStart, basePos + 1 + i, currentColor));

                    char lower = Character.toLowerCase(code);
                    if (lower == 'r') {
                        currentColor = activeTheme.string;
                    } else {
                        int col = mcColorFor(lower);
                        if (col != -1) currentColor = col;
                    }

                    spans.add(new Span(basePos + 1 + i, basePos + 1 + i + 2, dimColor(currentColor)));
                    segStart = i + 2;
                    i       += 2;
                    continue;
                }
            }
            i++;
        }

        if (segStart < content.length())
            spans.add(new Span(basePos + 1 + segStart, basePos + 1 + content.length(), currentColor));

        if (contentEnd < len)
            spans.add(new Span(basePos + contentEnd, basePos + len, activeTheme.string));
    }

    private int contextualColor(List<Tok> toks, int i) {
        SyntaxTheme th = activeTheme;
        Tok tok = toks.get(i);
        if (tok.type() == TokenType.WHITESPACE) return 0;
        if (tok.type() != TokenType.IDENT) return baseTokenColor(tok.type(), tok.text());

        Tok next   = nextNonWsTok(toks, i + 1);
        int prevI  = prevNonWsIdx(toks, i - 1);
        Tok prev   = prevI >= 0 ? toks.get(prevI) : null;

        if ("p".equals(tok.text())  && next != null && next.type() == TokenType.DOT)    return th.playerVar;
        if ("g".equals(tok.text())  && next != null && next.type() == TokenType.DOT)    return th.globalVar;
        if ("t".equals(tok.text())  && next != null && next.type() == TokenType.LBRACK) return th.teamVar;
        if ("ph".equals(tok.text()) && next != null && next.type() == TokenType.DOT)    return th.phVar;

        if (prev != null && prev.type() == TokenType.DOT) {
            int prevPrevI = prevNonWsIdx(toks, prevI - 1);
            Tok prevPrev  = prevPrevI >= 0 ? toks.get(prevPrevI) : null;

            if (prevPrev != null && "ph".equals(prevPrev.text())) {
                return Placeholder.searchByName(tok.text()) != null ? th.phNameValid : th.plain;
            }
            if (prevPrev != null && "p".equals(prevPrev.text())) return th.playerVar;
            if (prevPrev != null && "g".equals(prevPrev.text())) return th.globalVar;

            if (prevPrev != null && prevPrev.type() == TokenType.RBRACK) {
                int depth = 1;
                int k = prevPrevI - 1;
                while (k >= 0 && depth > 0) {
                    TokenType tt = toks.get(k).type();
                    if (tt == TokenType.RBRACK)      depth++;
                    else if (tt == TokenType.LBRACK) depth--;
                    k--;
                }
                Tok beforeBrack = prevNonWsTok(toks, k);
                if (beforeBrack != null && "t".equals(beforeBrack.text())) return th.teamVar;
            }
        }

        return baseTokenColor(tok.type(), tok.text());
    }

    private int baseTokenColor(TokenType t, String text) {
        SyntaxTheme th = activeTheme;
        return switch (t) {
            case FUNCTION, RETURN, LOOP,
                 IF, ELSE, REPEAT, EVENT, RANDOM, COMMAND, IMPORT, MENU, GLOBAL, CONST, ENUM -> th.keyword;
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
}
