package com.drakou111.neohouse.compiler.lexer;

import java.util.regex.Pattern;

public enum TokenType {
    COMMENT_BLOCK("/\\*(?:.|\\R)*?\\*/"),
    NOTE("///[^\\n]*"),
    COMMENT_LINE("//[^\\n]*"),

    FUNCTION("function(?![a-zA-Z0-9_$])"), RETURN("return(?![a-zA-Z0-9_$])"), LOOP("loop(?![a-zA-Z0-9_$])"),
    IF("if(?![a-zA-Z0-9_$])"), ELSE("else(?![a-zA-Z0-9_$])"), REPEAT("repeat(?![a-zA-Z0-9_$])"), EVENT("event(?![a-zA-Z0-9_$])"), MENU("menu(?![a-zA-Z0-9_$])"),
    LONG_CAST("\\(long\\)"), DOUBLE_CAST("\\(double\\)"), STRING_CAST("\\(string\\)"),
    RANDOM("random(?![a-zA-Z0-9_$])"), COMMAND("command(?![a-zA-Z0-9_$])"), IMPORT("import(?![a-zA-Z0-9_$])"), GLOBAL("global(?![a-zA-Z0-9_$])"), CONST("const(?![a-zA-Z0-9_$])"), ENUM("enum(?![a-zA-Z0-9_$])"),
    TRUE("true(?![a-zA-Z0-9_$])"), FALSE("false(?![a-zA-Z0-9_$])"),
    LONG("long(?![a-zA-Z0-9_$])"), DOUBLE("double(?![a-zA-Z0-9_$])"), STRING("string(?![a-zA-Z0-9_$])"), BOOL("bool(?![a-zA-Z0-9_$])"), VAR("var(?![a-zA-Z0-9_$])"),

    DOUBLE_VALUE("\\d(?:_?\\d)*\\.\\d(?:_?\\d)*(?:[dD])?"),
    // Hex (0xFF) and binary (0b101) before decimal; '_' may separate digits (1_000, 0xFF_FF).
    LONG_VALUE("0[xX][0-9a-fA-F](?:_?[0-9a-fA-F])*(?![0-9a-zA-Z_$])|0[bB][01](?:_?[01])*(?![0-9a-zA-Z_$])|\\d(?:_?\\d)*"),
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
    COMMA(","), SEMI(";"), COLON(":"), DOT("\\."), BAR("\\|"), QUESTION("\\?"),
    WHITESPACE("[ \\t\\r\\n]+"),

    CONST_IDENT("(?:PI|E|HALF_PI|TWO_PI|SQRT_TWO|SQRT_THREE|SQRT_FIVE|PHI)(?![a-zA-Z0-9_$])"),
    IDENT("[a-zA-Z_$][a-zA-Z0-9_$]*"),

    EOF(null),
    UNKNOWN(null);

    public final Pattern pattern;
    TokenType(String regex) {
        if (regex == null) {
            this.pattern = null;
            return;
        }
        // Anchoring is handled by Matcher.lookingAt() against a region in the lexer,
        // so no leading '^' is needed here.
        this.pattern = Pattern.compile(regex);
    }
}
