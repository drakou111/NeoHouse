package com.drakou111.neohouse.compilers.lexer;

import java.util.regex.Pattern;

public enum TokenType {
    COMMENT_BLOCK("/\\*(?:.|\\R)*?\\*/"),
    NOTE("///[^\\n]*"),
    COMMENT_LINE("//[^\\n]*"),

    FUNCTION("function"), RETURN("return"), CANCEL("cancel"), GLOBAL("global"),
    IF("if"), ELSE("else"), REPEAT("repeat"), EVENT("event"),
    DEFINE("define"), RANDOM("random"),
    TRUE("true"), FALSE("false"),
    LONG("long"), DOUBLE("double"), STRING("string"), BOOL("bool"), VAR("var"),

    DOUBLE_CONST("\\d+\\.\\d+(?:[dD])?"),
    LONG_CONST("\\d+"),
    STRING_CONST("\"([^\"\\\\]|\\\\.)*\""),

    EQ_EQ("=="), NOT_EQ("!="),
    PLUS_EQ("\\+="), MINUS_EQ("-="), STAR_EQ("\\*="), SLASH_EQ("/="), PERCENT_EQ("%="), TILDE_EQ("~="),
    PLUS_PLUS("\\+\\+"), MINUS_MINUS("--"),

    EQ("="), LT("<"), LE("<="), GT(">"), GE(">="),
    NOT("!"), AND("&&"), OR("\\|\\|"),
    PLUS("\\+"), MINUS("-"), STAR("\\*"), SLASH("/"), PERCENT("%"),

    LPAREN("\\("), RPAREN("\\)"), LBRACE("\\{"), RBRACE("\\}"),
    LBRACK("\\["), RBRACK("\\]"),
    COMMA(","), SEMI(";"), DOT("\\."), BAR("\\|"), BANG("\\!"),
    WHITESPACE("[ \\t\\r\\n]+"),

    CONST_IDENT("PI|E|HALF_PI|TWO_PI|SQRT_TWO|SQRT_THREE|SQRT_FIVE|PHI"),
    IDENT("[a-zA-Z_][a-zA-Z0-9_]*"),

    EOF(null);

    public final Pattern pattern;
    TokenType(String regex) {
        if (regex == null) {
            this.pattern = null;
            return;
        }
        this.pattern = Pattern.compile("^" + regex);
    }
}
