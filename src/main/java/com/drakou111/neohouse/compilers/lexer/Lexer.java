package com.drakou111.neohouse.compilers.lexer;

import java.util.*;
import java.util.regex.*;

public class Lexer {
    private final String input;
    private int position = 0;

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
                if (!m.find()) continue;

                String lexeme = m.group();
                position += lexeme.length();
                matched = true;

                // Skip whitespace and comments
                if (tt == TokenType.WHITESPACE
                        || tt == TokenType.COMMENT_LINE
                        || tt == TokenType.COMMENT_BLOCK) {
                    break;
                }

                // Reclassify IDENT as keyword if needed
                if (tt == TokenType.IDENT) {
                    switch (lexeme) {
                        case "function":  tt = TokenType.FUNCTION;  break;
                        case "broadcast": tt = TokenType.BROADCAST; break;
                        case "return":    tt = TokenType.RETURN;    break;
                        case "if":        tt = TokenType.IF;        break;
                        case "else":      tt = TokenType.ELSE;      break;
                        case "repeat":    tt = TokenType.REPEAT;    break;
                        case "event":     tt = TokenType.EVENT;     break;
                        case "define":    tt = TokenType.DEFINE;    break;
                        case "random":    tt = TokenType.RANDOM;    break;
                        case "true":      tt = TokenType.TRUE;      break;
                        case "false":     tt = TokenType.FALSE;     break;
                    }
                }

                tokens.add(new Token(tt, lexeme));
                break;
            }

            if (!matched) {
                throw new RuntimeException(
                        "Unknown token at position " + position + ": " + rest);
            }
        }
        return tokens;
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
