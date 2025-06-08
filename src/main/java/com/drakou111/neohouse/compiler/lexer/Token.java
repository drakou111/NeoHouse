package com.drakou111.neohouse.compiler.lexer;

public class Token {
    public final TokenType type;
    public final String text;
    public final String lineContent;
    public final int line;
    public final int column;

    public Token(TokenType type, String text, String lineContent, int line, int column) {
        this.type = type;
        this.text = text;
        this.lineContent = lineContent;
        this.line = line;
        this.column = column;
    }

    @Override public String toString() {
        return type + "('" + text + "')";
    }
}
