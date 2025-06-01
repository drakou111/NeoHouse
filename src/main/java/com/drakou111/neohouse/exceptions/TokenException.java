package com.drakou111.neohouse.exceptions;

import com.drakou111.neohouse.compilers.lexer.Token;

public class TokenException extends CompilingException {
    public TokenException(Token token, String message) {
        super(token, message);
    }
}
