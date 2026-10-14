package com.drakou111.neohouse.exceptions;

import com.drakou111.neohouse.compiler.lexer.Token;

public class SemanticException extends CompilingException {
    public SemanticException(Token token, String message) {
        super(token, message);
    }
}
