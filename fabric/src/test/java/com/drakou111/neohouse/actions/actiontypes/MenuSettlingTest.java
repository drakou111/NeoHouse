package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.exceptions.InterruptActionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuSettlingTest {

    private static final int BASE_TICKS = 4;
    private static final List<String> EMPTY = List.of("", "", "");
    private static final List<String> PARTIAL = List.of("a", "", "");
    private static final List<String> FULL = List.of("a", "b", "Next Page");
    private static final int ARROW = 2;
    private static final List<String> ARROW_LIFTED = List.of("a", "b", "");
    private static final List<String> NEXT_PAGE = List.of("c", "d", "Next Page");

    /** Feeds {@code snapshot} until the settling says loaded; returns how many ticks that took. */
    private static int ticksUntilSettled(MenuSettling settling, List<String> snapshot) {
        for (int tick = 1; tick <= 1000; tick++)
            if (settling.update(snapshot)) return tick;
        throw new AssertionError("never settled");
    }

    @Test
    void stableMenuSettlesAfterTheBaseWindow() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);

        assertThat(ticksUntilSettled(settling, FULL)).isEqualTo(BASE_TICKS + 1);
    }

    @Test
    void emptyMenuNeverSettles() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);

        for (int tick = 0; tick < 100; tick++) assertThat(settling.update(EMPTY)).isFalse();
    }

    @Test
    void expectedLagWidensTheWindow() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);
        settling.expectLag(12);

        assertThat(settling.requiredStableTicks()).isEqualTo(12);
        assertThat(ticksUntilSettled(settling, FULL)).isEqualTo(13);
    }

    @Test
    void slowBurstsWidenTheWindowToTwiceTheLongestPause() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);
        settling.update(PARTIAL);
        for (int tick = 0; tick < 3; tick++) assertThat(settling.update(PARTIAL)).isFalse();
        // The rest arrives 4 ticks after the first part, which a fixed 4-tick window would have missed.
        assertThat(settling.update(FULL)).isFalse();

        assertThat(settling.requiredStableTicks()).isEqualTo(8);
    }

    @Test
    void windowIsCapped() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);
        settling.expectLag(10_000);

        assertThat(settling.requiredStableTicks()).isEqualTo(MenuSettling.MAX_STABLE_TICKS);
    }

    @Test
    void resetClearsTheLearnedLag() {
        MenuSettling settling = new MenuSettling(BASE_TICKS);
        settling.expectLag(20);
        settling.reset();

        assertThat(settling.requiredStableTicks()).isEqualTo(BASE_TICKS);
    }

    @Test
    void liftingTheClickedArrowIsNotAPageTurn() {
        // The client shows the click at once by moving the arrow onto the cursor; the page is still the old one.
        PageTurn turn = new PageTurn("function list");
        turn.start(FULL, ARROW);

        for (int tick = 0; tick < 50; tick++) assertThat(turn.hasTurned(ARROW_LIFTED, 100)).isFalse();
        assertThat(turn.hasTurned(FULL, 100)).isFalse();
        assertThat(turn.hasTurned(NEXT_PAGE, 100)).isTrue();
    }

    @Test
    void closedMenuCountsAsAChange() {
        PageTurn turn = new PageTurn("function list");
        turn.start(FULL, ARROW);

        assertThat(turn.hasTurned(List.of(), 100)).isTrue();
    }

    @Test
    void pageTurnReportsTheResponseTime() {
        PageTurn turn = new PageTurn("function list");
        turn.start(FULL, ARROW);
        for (int tick = 0; tick < 7; tick++) assertThat(turn.hasTurned(FULL, 100)).isFalse();

        assertThat(turn.hasTurned(PARTIAL, 100)).isTrue();
        assertThat(turn.responseTicks()).isEqualTo(7);
    }

    @Test
    void pageThatNeverTurnsIsAnErrorNotTheLastPage() {
        PageTurn turn = new PageTurn("function list");
        turn.start(FULL, ARROW);
        for (int tick = 0; tick < 10; tick++) turn.hasTurned(FULL, 10);

        assertThatThrownBy(() -> turn.hasTurned(FULL, 10))
                .isInstanceOf(InterruptActionException.class)
                .hasMessageContaining("next page of the function list");
    }
}
