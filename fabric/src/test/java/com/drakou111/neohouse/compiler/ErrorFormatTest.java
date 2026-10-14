package com.drakou111.neohouse.compiler;

import com.drakou111.neohouse.compiler.lexer.Token;
import com.drakou111.neohouse.compiler.lexer.TokenType;
import com.drakou111.neohouse.exceptions.CompilingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorFormatTest {

    @Test
    void caretsSitUnderTheTokenAndControlCharactersAreRemoved() {
        Token token = new Token(TokenType.IDENT, "foo", "\tp.x = foo;\r", 3, 8, "lib/a.nh");

        String[] lines = CompilingException.format(token, "Unknown thing").split("\n");

        assertThat(lines[0]).isEqualTo("lib/a.nh, line 3, col 8: Unknown thing");
        assertThat(lines[1]).isEqualTo("    p.x = foo;");
        assertThat(lines[2]).isEqualTo("          ^^^");
    }

    @Test
    void unnamedScriptHasNoFileName() {
        Token token = new Token(TokenType.IDENT, "x", "x", 1, 1);

        assertThat(CompilingException.location(token)).isEqualTo("At line 1, col 1");
    }
}
