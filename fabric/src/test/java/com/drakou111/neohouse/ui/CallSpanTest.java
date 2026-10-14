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

    private static boolean continues(int callLine, int lineIndex, String... lines) {
        return CallSpan.continuesCall(List.of(lines), callLine, lineIndex, MAX_LINES);
    }

    @Test
    void indentedArgumentsContinueACallWhoseParenIsNotTypedYet() {
        assertThat(continues(0, 1, "playSound(", "    \"x\",")).isTrue();
        assertThat(continues(0, 2, "playSound(", "    \"x\",", "    1.0")).isTrue();
        assertThat(continues(1, 2, "function f() {", "    playSound(", "        \"x\"", "}")).isTrue();
    }

    @Test
    void sameIndentStatementAfterAnUnfinishedCallDoesNotContinueIt() {
        assertThat(continues(1, 2, "function f() {", "    sin(", "    p.x = 1", "}")).isFalse();
        assertThat(continues(0, 1, "sin(", "p.x = 1")).isFalse();
    }

    @Test
    void shallowerBlockEndDoesNotContinueTheCall() {
        assertThat(continues(1, 2, "function f() {", "    sin(", "}")).isFalse();
    }

    @Test
    void unindentedArgumentsContinueOnceTheParenIsTyped() {
        assertThat(continues(0, 1, "foo(", "1,", "2)")).isTrue();
    }

    private static List<CallSpan.ArgSlot> slots(String line, int cursorColumn) {
        return CallSpan.argumentSlots(line, line.indexOf('(') + 1, 0, cursorColumn);
    }

    @Test
    void filledArgumentsAreLabelledWhereTheirTextStarts() {
        assertThat(slots("foo(1, \"a,b\", bar(2, 3))", -1)).containsExactly(
                new CallSpan.ArgSlot(4, 0), new CallSpan.ArgSlot(7, 1), new CallSpan.ArgSlot(14, 2));
    }

    @Test
    void emptySlotAfterACommaIsLabelledWhileTheCursorIsInIt() {
        String line = "foo(1, ";
        assertThat(slots(line, line.length())).containsExactly(
                new CallSpan.ArgSlot(4, 0), new CallSpan.ArgSlot(7, 1));
        assertThat(slots("foo(1,", 6)).containsExactly(
                new CallSpan.ArgSlot(4, 0), new CallSpan.ArgSlot(6, 1));
    }

    @Test
    void emptySlotIsNotLabelledWhenTheCursorIsElsewhere() {
        assertThat(slots("foo(1, ", -1)).containsExactly(new CallSpan.ArgSlot(4, 0));
        assertThat(slots("foo(1, ", 2)).containsExactly(new CallSpan.ArgSlot(4, 0));
    }

    @Test
    void emptyFirstArgumentIsLabelledRightAfterTheParen() {
        assertThat(slots("foo(", 4)).containsExactly(new CallSpan.ArgSlot(4, 0));
        assertThat(slots("foo()", 4)).containsExactly(new CallSpan.ArgSlot(4, 0));
    }

    @Test
    void anEmptySlotDoesNotShiftTheLaterLabels() {
        assertThat(slots("foo(, x)", -1)).containsExactly(new CallSpan.ArgSlot(6, 1));
    }

    @Test
    void continuationLineStartsAtTheGivenArgument() {
        assertThat(CallSpan.argumentSlots("    \"x\", ", 0, 2, 9)).containsExactly(
                new CallSpan.ArgSlot(4, 2), new CallSpan.ArgSlot(9, 3));
    }

    @Test
    void statementLineIsNotPartOfTheCall() {
        assertThat(CallSpan.argumentSlots("p.x = 1;", 0, 1, -1)).isEmpty();
    }

    @Test
    void scanStopsAfterMaxLines() {
        assertThat(CallSpan.closesBeforeStatementEnds(List.of("foo(", "1,", "2,", ")"), 1, 2)).isFalse();
    }
}
