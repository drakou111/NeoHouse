package com.drakou111.neohouse.compiler.lexer;

import com.drakou111.neohouse.exceptions.TokenException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Lexer {
    private final String input;
    private final String[] lines;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String input) {
        this.input = input;
        this.lines = input.split("\n", -1);
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        // Reuse one Matcher per token type across the whole input (via Matcher.region),
        // instead of allocating a fresh substring + matchers at every position. This keeps
        // tokenization linear rather than O(n²) on large scripts.
        TokenType[] types = TokenType.values();
        Matcher[] matchers = new Matcher[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].pattern != null) matchers[i] = types[i].pattern.matcher(input);
        }
        int length = input.length();

        while (position < length) {
            boolean matched = false;

            for (int i = 0; i < types.length; i++) {
                Matcher m = matchers[i];
                if (m == null) continue;

                m.region(position, length);
                if (!m.lookingAt()) continue;

                TokenType tt = types[i];
                String lexeme = m.group();
                matched = true;

                int tokenLine = line;
                int tokenColumn = column;

                // Track lines and columns
                for (int c = 0; c < lexeme.length(); c++) {
                    if (lexeme.charAt(c) == '\n') {
                        line++;
                        column = 1;
                    } else {
                        column++;
                    }
                }

                position += lexeme.length();

                // Skip whitespace and comments
                if (tt == TokenType.WHITESPACE || tt == TokenType.COMMENT_LINE || tt == TokenType.COMMENT_BLOCK) {
                    break;
                }

                String lineContent = "";
                if (tokenLine >= 1 && tokenLine <= lines.length) {
                    lineContent = lines[tokenLine - 1];
                }

                tokens.add(new Token(tt, lexeme, lineContent, tokenLine, tokenColumn));
                break;
            }

            if (!matched) {
                throw errorWithPointer();
            }
        }
        tokens.add(new Token(TokenType.EOF, "", "", line, column));
        return tokens;
    }

    private TokenException errorWithPointer() {
        String lineContent = (line >= 1 && line <= lines.length) ? lines[line - 1] : "";
        return new TokenException(new Token(TokenType.UNKNOWN, input.charAt(position) + "", lineContent, line, column), "Tokenization error");
    }
}
