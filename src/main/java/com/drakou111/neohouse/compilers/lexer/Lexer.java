package com.drakou111.neohouse.compilers.lexer;

import com.drakou111.neohouse.exceptions.TokenException;

import java.util.*;
import java.util.regex.*;

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

                tokens.add(new Token(tt, lexeme));
                break;
            }

            if (!matched) {
                throw errorWithPointer();
            }
        }
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

    public static void main(String[] args) {
        String code = """
            /// doc comment
            function foo(ph.server.name, PI) {
              broadcast title("Hi", "All", 1, 0, 1);
              p.x += 3.14D;
              repeat (20) { // repeat block
                xp(5);
              }
            }
            """;

        Lexer lex = new Lexer(code);
        for (Token tok : lex.tokenize()) {
            System.out.println(tok);
        }
    }
}
