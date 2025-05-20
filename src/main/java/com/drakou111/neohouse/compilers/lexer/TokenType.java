package com.drakou111.neohouse.compilers.lexer;

import java.util.regex.Pattern;

public enum TokenType {
    NOTE("///[^\\n]*"), COMMENT_LINE("//[^\\n]*"), COMMENT_BLOCK("/\\*(?:.|\\R)*?\\*/"),
    FUNCTION("function"), BROADCAST("broadcast"), RETURN("return"),
    IF("if"), ELSE("else"), REPEAT("repeat"), EVENT("event"),
    DEFINE("define"), RANDOM("random"),
    TRUE("true"), FALSE("false"),
    LONG("long"), DOUBLE("double"), STRING("string"), BOOL("bool"), VAR("var"),

    CONST_IDENT("PI|E|HALF_PI|TWO_PI"),

    DOUBLE_CONST("\\d+\\.\\d+(?:[dD])?"),
    LONG_CONST("\\d+"),
    STRING_CONST("\"([^\"\\\\]|\\\\.)*\""),

    P_PREFIX("p(\\.[a-zA-Z_][a-zA-Z0-9_.]*)+"),
    G_PREFIX("g(\\.[a-zA-Z_][a-zA-Z0-9_.]*)+"),
    T_PREFIX("t(\\.[a-zA-Z_][a-zA-Z0-9_.]*)+"),
    PH_PREFIX("ph(\\.[a-zA-Z_][a-zA-Z0-9_.]*)+"),

    EQ_EQ("=="), NOT_EQ("!="),
    PLUS_EQ("\\+="), MINUS_EQ("-="), STAR_EQ("\\*="), SLASH_EQ("/="), PERCENT_EQ("%="), TILDE_EQ("~="),
    PLUS_PLUS("\\+\\+"), MINUS_MINUS("--"),
    EQ("="), LT("<"), LE("<="), GT(">"), GE(">="),
    NOT("!"), AND("&&"), OR("\\|\\|"),
    PLUS("\\+"), MINUS("-"), STAR("\\*"), SLASH("/"), PERCENT("%"),
    LPAREN("\\("), RPAREN("\\)"), LBRACE("\\{"), RBRACE("\\}"),
    LBRACK("\\["), RBRACK("\\]"),
    COMMA(","), SEMI(";"), DOT("\\."), BAR("\\|"),

    WHITESPACE("[ \\t\\r\\n]+"),

    IDENT("[a-zA-Z_][a-zA-Z0-9_]*"),
    UNKNOWN(".");

    public final Pattern pattern;
    TokenType(String regex) {
        this.pattern = Pattern.compile("^" + regex);
    }
}
