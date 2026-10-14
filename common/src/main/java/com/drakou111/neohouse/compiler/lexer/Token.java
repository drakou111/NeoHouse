package com.drakou111.neohouse.compiler.lexer;

public class Token {
    /** File name used for tokens of a script that isn't backed by a file (and for synthetic tokens). */
    public static final String UNNAMED_FILE = "<script>";

    public final TokenType type;
    public final String text;
    public final String lineContent;
    public final int line;
    /** 1-based column of the token's first character. */
    public final int column;
    /** Script-relative path of the file this token was read from, e.g. {@code lib/trig.nh}. */
    public final String file;

    public Token(TokenType type, String text, String lineContent, int line, int column) {
        this(type, text, lineContent, line, column, UNNAMED_FILE);
    }

    public Token(TokenType type, String text, String lineContent, int line, int column, String file) {
        this.type = type;
        this.text = text;
        this.lineContent = lineContent;
        this.line = line;
        this.column = column;
        this.file = file;
    }

    @Override public String toString() {
        return type + "('" + text + "')";
    }
}
