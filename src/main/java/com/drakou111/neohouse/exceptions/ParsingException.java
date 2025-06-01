package com.drakou111.neohouse.exceptions;

import com.drakou111.neohouse.compilers.lexer.Token;

public class ParsingException extends CompilingException {
    public ParsingException(Token token, String message) {
        super(token, message);
    }
}
