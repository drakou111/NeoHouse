package com.drakou111.neohouse.exceptions;

import com.drakou111.neohouse.compiler.lexer.Token;

public class CompilingException extends RuntimeException {
    private static final String TAB_AS_SPACES = "    ";

    public final Token errorToken;
    /** The bare problem description, without the location header or the source excerpt. */
    public final String detail;

    public CompilingException(Token token, String message) {
        super(token == null ? message : format(token, message));
        this.errorToken = token;
        this.detail = message;
    }

    /** {@code file, line L, col C: message}, followed by the offending line and a caret marker under the token. */
    public static String format(Token token, String message) {
        String header = location(token) + ": " + message;
        if (token.lineContent == null || token.lineContent.isBlank()) return header;
        return header + "\n" + printable(token.lineContent) + "\n" + caretLine(token);
    }

    public static String location(Token token) {
        String place = "line " + token.line + ", col " + token.column;
        return Token.UNNAMED_FILE.equals(token.file) ? "At " + place : token.file + ", " + place;
    }

    private static String caretLine(Token token) {
        int start = Math.max(0, Math.min(token.column - 1, token.lineContent.length()));
        String indent = printable(token.lineContent.substring(0, start)).replaceAll("\\S", " ");
        int length = Math.max(1, token.text == null ? 1 : token.text.length());
        return indent + "^".repeat(length);
    }

    private static String printable(String line) {
        return line.replace("\r", "").replace("\t", TAB_AS_SPACES);
    }
}
