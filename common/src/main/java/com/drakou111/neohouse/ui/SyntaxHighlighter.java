package com.drakou111.neohouse.ui;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.SyntaxTheme;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.constants.AddAction;
import com.drakou111.neohouse.constants.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SyntaxHighlighter {

    // ── Records ───────────────────────────────────────────────────────────────

    record Span(int start, int end, int color) {}

    private record Tok(TokenType type, String text, int pos) {}

    // ── State ─────────────────────────────────────────────────────────────────

    private SyntaxTheme activeTheme = SyntaxTheme.VSCODE_DARK;

    // ── Main API ──────────────────────────────────────────────────────────────

    public List<Span> buildSpans(String src) {
        List<Span> spans = new ArrayList<>();
        activeTheme = SyntaxTheme.byId(NeoHouseConfig.get().syntaxTheme);

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

        return spans;
    }

    // ── Color logic ───────────────────────────────────────────────────────────

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
            case FUNCTION, RETURN, LOOP,
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

    // ── Static helpers ────────────────────────────────────────────────────────

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
