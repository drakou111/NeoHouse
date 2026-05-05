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
        while (position < input.length()) {
            String rest = input.substring(position);
            boolean matched = false;

            for (TokenType tt : TokenType.values()) {
                if (tt.pattern == null) continue;

                Matcher m = tt.pattern.matcher(rest);
                if (!m.lookingAt()) continue;

                String lexeme = m.group();
                matched = true;

                int tokenLine = line;

                // Track lines and columns
                for (char c : lexeme.toCharArray()) {
                    if (c == '\n') {
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

                tokens.add(new Token(tt, lexeme, lineContent, tokenLine, column));
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
