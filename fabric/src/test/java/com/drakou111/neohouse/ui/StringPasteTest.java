package com.drakou111.neohouse.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringPasteTest {

    private static String pasteAtBar(String textWithBar, String pasted) {
        int offset = textWithBar.indexOf('|');
        String text = textWithBar.replace("|", "");
        return StringPaste.forOffset(text, offset, pasted);
    }

    @Test
    void quotesPastedIntoAStringAreEscaped() {
        // message("some |");  +  "text inside"  ->  \"text inside\"
        assertThat(pasteAtBar("message(\"some |\");", "\"text inside\"")).isEqualTo("\\\"text inside\\\"");
    }

    @Test
    void escapesAlreadyInThePasteGoOneLevelDeeper() {
        // say("a \"b\"")  ->  say(\"a \\\"b\\\"\")
        assertThat(pasteAtBar("message(\"|\");", "say(\"a \\\"b\\\"\")"))
                .isEqualTo("say(\\\"a \\\\\\\"b\\\\\\\"\\\")");
    }

    @Test
    void outsideAStringThePasteIsKept() {
        assertThat(pasteAtBar("message(\"a\"|);", "\"b\"")).isEqualTo("\"b\"");
        assertThat(pasteAtBar("|message(\"a\");", "\"b\"")).isEqualTo("\"b\"");
    }

    @Test
    void escapedQuotesAndCommentsDontConfuseIt() {
        // message("a \" b"); x   -> after the string
        String closed = "message(\"a \\\" b\"); x";
        assertThat(StringPaste.isInsideString(closed, closed.indexOf('x'))).isFalse();
        // message("a \" |        -> still inside
        String open = "message(\"a \\\" ";
        assertThat(StringPaste.isInsideString(open, open.length())).isTrue();
        String lineComment = "// it's \"quoted\n x";
        assertThat(StringPaste.isInsideString(lineComment, lineComment.indexOf('x'))).isFalse();
        String blockComment = "/* \" */ x";
        assertThat(StringPaste.isInsideString(blockComment, blockComment.indexOf('x'))).isFalse();
    }

    @Test
    void theEndOfAnUnclosedStringIsInside() {
        assertThat(StringPaste.isInsideString("message(\"abc", 12)).isTrue();
    }
}
