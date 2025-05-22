package com.drakou111.neohouse.compilers.lexer;

import java.util.regex.Pattern;

public enum TokenType {
    COMMENT_BLOCK("/\\*(?:.|\\R)*?\\*/"),
    NOTE("///[^\\n]*"),
    COMMENT_LINE("//[^\\n]*"),

    FUNCTION("function"), BROADCAST("broadcast"), RETURN("return"),
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
    COMMA(","), SEMI(";"), DOT("\\."), BAR("\\|"),
    WHITESPACE("[ \\t\\r\\n]+"),

    CONST_IDENT("PI|E|HALF_PI|TWO_PI"),
    IDENT("[a-zA-Z_][a-zA-Z0-9_]*");

    public final Pattern pattern;
    TokenType(String regex) {
        this.pattern = Pattern.compile("^" + regex);
    }
}
