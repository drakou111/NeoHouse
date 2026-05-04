package com.drakou111.neohouse.compiler.lexer;

import java.util.regex.Pattern;

public enum TokenType {
    COMMENT_BLOCK("/\\*(?:.|\\R)*?\\*/"),
    NOTE("///[^\\n]*"),
    COMMENT_LINE("//[^\\n]*"),

    FUNCTION("function"), RETURN("return"), CANCEL("cancel"), LOOP("loop"), GLOBAL("global"),
    IF("if"), ELSE("else"), REPEAT("repeat"), EVENT("event"), MENU("menu"),
    LONG_CAST("\\(long\\)"), DOUBLE_CAST("\\(double\\)"), STRING_CAST("\\(string\\)"),
    RANDOM("random"), COMMAND("command"), IMPORT("import"), ITEM("item"),
    TRUE("true"), FALSE("false"),
    LONG("long"), DOUBLE("double"), STRING("string"), BOOL("bool"), VAR("var"),

    DOUBLE_VALUE("\\d+\\.\\d+(?:[dD])?"),
    LONG_VALUE("\\d+"),
    STRING_VALUE("\"([^\"\\\\]|\\\\.)*\""),

    EQ_EQ("=="),
    PLUS_EQ("\\+="), MINUS_EQ("-="), STAR_EQ("\\*="), SLASH_EQ("/="), PERCENT_EQ("%="), TILDE_EQ("~="),
    UNSIGNED_RIGHT_SHIFT_EQ(">>>="), RIGHT_SHIFT_EQ(">>="), LEFT_SHIFT_EQ("<<="),
    AMP_EQ("&="), CARET_EQ("\\^="), BAR_EQ("\\|="),
    PLUS_PLUS("\\+\\+"), MINUS_MINUS("--"),

    UNSIGNED_RIGHT_SHIFT(">>>"), RIGHT_SHIFT(">>"), LEFT_SHIFT("<<"),
    LE("<="), GE(">="), EQ("="), LT("<"), GT(">"),
    NOT("!"), AND("&&"), OR("\\|\\|"),
    PLUS("\\+"), MINUS("-"), STAR("\\*"), SLASH("/"), PERCENT("%"),
    AMPERSAND("&"), CARET("\\^"),

    LPAREN("\\("), RPAREN("\\)"), LBRACE("\\{"), RBRACE("\\}"),
    LBRACK("\\["), RBRACK("\\]"),
    COMMA(","), SEMI(";"), COLON(":"), DOT("\\."), BAR("\\|"),
    WHITESPACE("[ \\t\\r\\n]+"),

    CONST_IDENT("PI|E|HALF_PI|TWO_PI|SQRT_TWO|SQRT_THREE|SQRT_FIVE|PHI"),
    IDENT("[a-zA-Z_][a-zA-Z0-9_]*"),

    EOF(null),
    UNKNOWN(null);

    public final Pattern pattern;
    TokenType(String regex) {
        if (regex == null) {
            this.pattern = null;
            return;
        }
        this.pattern = Pattern.compile("^" + regex);
    }
}
