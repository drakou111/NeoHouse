package com.drakou111.neohouse.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CallSpanTest {

    private static final int MAX_LINES = 30;

    private static boolean closes(int fromLine, String... lines) {
        return CallSpan.closesBeforeStatementEnds(List.of(lines), fromLine, MAX_LINES);
    }

    @Test
    void callSplitOverLinesCloses() {
        assertThat(closes(1, "playSound(", "    \"x\",", "    1.0", ");")).isTrue();
    }

    @Test
    void nestedCallsInsideTheArgumentsDoNotCloseIt() {
        assertThat(closes(1, "foo(", "    bar(1), baz(2)", ")")).isTrue();
        assertThat(closes(1, "foo(", "    bar(1);")).isFalse();
    }

    @Test
    void unfinishedCallFollowedByAStatementDoesNotClose() {
        assertThat(closes(1, "sin(", "p.x = cos(1);")).isFalse();
    }

    @Test
    void unfinishedCallFollowedByABlockEndDoesNotClose() {
        assertThat(closes(1, "    sin(", "}")).isFalse();
    }

    @Test
    void unfinishedCallFollowedByABlockStartDoesNotClose() {
        assertThat(closes(1, "sin(", "if (p.x == 1) {", "}")).isFalse();
    }

    @Test
    void unfinishedCallFollowedByAStatementWithoutSemicolonDoesNotClose() {
        assertThat(closes(1, "sin(", "p.x = 1", "}")).isFalse();
    }

    @Test
    void bracketsInStringsAndCommentsAreIgnored() {
        assertThat(closes(1, "foo(", "    \"};\", // ; }", ")")).isTrue();
    }

    @Test
    void nothingAfterTheCallDoesNotClose() {
        assertThat(closes(1, "foo(", "    1,", "")).isFalse();
    }

    @Test
    void scanStopsAfterMaxLines() {
        assertThat(CallSpan.closesBeforeStatementEnds(List.of("foo(", "1,", "2,", ")"), 1, 2)).isFalse();
    }
}
