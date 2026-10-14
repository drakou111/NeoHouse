package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.lexer.Lexer;
import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

    @Test
    void tokenColumnIsOneBasedStart() {
        List<Token> tokens = new Lexer("long  abc = 5;").tokenize();

        Token ident = tokens.get(1);
        assertThat(ident.type).isEqualTo(TokenType.IDENT);
        assertThat(ident.column).isEqualTo(7);
    }
}
