package com.drakou111.neohouse.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeWrapTest {

    // One pixel per character.
    private static int width(String text) {
        return text.length();
    }

    @Test
    void aLineThatFitsIsKept() {
        assertThat(CodeWrap.wrap("f(a, b)", 20, CodeWrapTest::width)).containsExactly("f(a, b)");
    }

    @Test
    void aLongLineBreaksAfterCommasWithAnIndentedContinuation() {
        assertThat(CodeWrap.wrap("dropItem(item, location, naturally, merge)", 25, CodeWrapTest::width))
                .containsExactly("dropItem(item, location,", "    naturally, merge)");
    }

    @Test
    void theContinuationKeepsTheLinesOwnIndent() {
        assertThat(CodeWrap.wrap("  f(aaaa, bbbb, cccc)", 14, CodeWrapTest::width))
                .containsExactly("  f(aaaa,", "      bbbb,", "      cccc)");
    }

    @Test
    void commasInsideTextAreNotBreakPoints() {
        assertThat(CodeWrap.wrap("message(\"Hi, there, you\", 1)", 12, CodeWrapTest::width))
                .containsExactly("message(\"Hi, there, you\",", "    1)");
    }

    @Test
    void aPieceWithoutCommasStaysWhole() {
        assertThat(CodeWrap.wrap("averyveryverylongname()", 5, CodeWrapTest::width)).containsExactly("averyveryverylongname()");
    }
}
