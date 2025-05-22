package com.drakou111.neohouse.compilers.parser;
import com.drakou111.neohouse.compilers.lexer.Token;
import com.drakou111.neohouse.compilers.lexer.TokenType;
import com.drakou111.neohouse.exceptions.ParseException;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean isAtEnd() {
        return pos >= tokens.size();
    }

    private int currentPosition() {
        return pos;
    }

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private boolean match(TokenType... types) {
        Token t = peek();
        if (t != null) {
            for (TokenType type : types) {
                if (t.type == type) {
                    pos++;
                    return true;
                }
            }
        }
        return false;
    }

    private Token expect(TokenType type) throws ParseException {
        Token t = peek();
        if (t == null || t.type != type) {
            throw new ParseException(getErrorString(type.toString()));
        }
        pos++;
        return t;
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private boolean check(TokenType type) {
        Token t = peek();
        return t != null && t.type == type;
    }


    private String getErrorString(String expected) {
        Token peek = peek();
        return "Expected " + expected + " at " + currentPosition() + " but got " + (peek == null ? "EOF" : peek.type);
    }
}

