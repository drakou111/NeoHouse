package com.drakou111.neohouse.exceptions;

import com.drakou111.neohouse.compiler.lexer.Token;

public class CompilingException extends RuntimeException {
    public final Token errorToken;

    public CompilingException(Token token, String message) {
        super(token == null ? message : "\n[ERROR] An error occurred while compiling at '" + token.text + "', at line " + token.line + " & col " + token.column + ": " + message + "\n[ERROR] " + token.lineContent + "\n" + arrowText(token) + "\n");
        this.errorToken = token;
    }

    private static String arrowText(Token token) {
        int indent = token.column - token.text.length() - 1 + "[ERROR] ".length();
        int length = token.text.length();
        if (indent < 0) indent = 0;
        return " ".repeat(indent) + "^".repeat(length);
    }
}
