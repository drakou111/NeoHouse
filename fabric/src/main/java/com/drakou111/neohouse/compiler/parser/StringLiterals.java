package com.drakou111.neohouse.compiler.parser;

import com.drakou111.neohouse.compiler.lexer.Token;

/** Reads the text of a string literal token: the quotes are dropped and \" and \\ become " and \. */
final class StringLiterals {

    private StringLiterals() {
    }

    static String contentOf(Token token) {
        String raw = token.text.substring(1, token.text.length() - 1);
        if (raw.indexOf('\\') < 0) return raw;

        StringBuilder content = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            char next = i + 1 < raw.length() ? raw.charAt(i + 1) : 0;
            // Other escapes are kept as written, so text like "\n" reaches Housing unchanged.
            if (c == '\\' && (next == '"' || next == '\\')) {
                content.append(next);
                i++;
            } else {
                content.append(c);
            }
        }
        return content.toString();
    }
}
