package com.drakou111.neohouse.compilers.lexer;

import com.drakou111.neohouse.exceptions.TokenException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Lexer {
    private final String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (position < input.length()) {
            String rest = input.substring(position);
            boolean matched = false;

            for (TokenType tt : TokenType.values()) {
                Matcher m = tt.pattern.matcher(rest);
                if (!m.lookingAt()) continue;

                String lexeme = m.group();
                matched = true;

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

                tokens.add(new Token(tt, lexeme, line, column));
                break;
            }

            if (!matched) {
                throw errorWithPointer();
            }
        }
        tokens.add(new Token(TokenType.EOF, null, line, column));
        return tokens;
    }

    private TokenException errorWithPointer() {
        int lineStart = position;
        while (lineStart > 0 && input.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        int lineEnd = position;
        while (lineEnd < input.length() && input.charAt(lineEnd) != '\n') {
            lineEnd++;
        }

        String errorLine = input.substring(lineStart, lineEnd).replace("\t", " ");
        int caretPos = position - lineStart;

        StringBuilder message = new StringBuilder();
        message.append("Unknown token at line ").append(line).append(", column ").append(column).append(":\n");
        message.append(errorLine).append("\n");
        message.append(" ".repeat(Math.max(0, caretPos))).append("^\n");

        return new TokenException(message.toString());
    }
}
