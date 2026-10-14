package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.exceptions.InterruptActionException;

import java.util.List;

/**
 * Waits for a clicked next-page arrow to swap the page in. The arrow was only clicked because it
 * said there is a next page, so a page that never changes means lag, not "last page": guessing
 * there would make a search miss items that exist, so it fails with a clear error instead.
 *
 * <p>The clicked slot itself is ignored: the client shows the click at once by lifting the arrow
 * onto the cursor, so that slot changes long before the server sends the next page. Counting it
 * made a laggy page turn look done while the old page was still showing.</p>
 */
final class PageTurn {
    private final String listDescription;
    private List<String> pageBefore = null;
    private int clickedPosition = -1;
    private int waitedTicks = 0;

    PageTurn(String listDescription) {
        this.listDescription = listDescription;
    }

    /** {@code clickedPosition} is the arrow's index in the page lists passed to {@link #hasTurned}. */
    void start(List<String> pageBefore, int clickedPosition) {
        this.pageBefore = pageBefore;
        this.clickedPosition = clickedPosition;
        this.waitedTicks = 0;
    }

    /**
     * Call once per tick with the current page; true once it differs from the page before the click
     * anywhere but the clicked slot (or the menu was closed or replaced by one of another size).
     */
    boolean hasTurned(List<String> page, int timeoutTicks) {
        if (!sameApartFromClickedSlot(page)) return true;
        if (++this.waitedTicks > timeoutTicks)
            throw new InterruptActionException(("The next page of the %s didn't load within %d ticks. The server may be lagging: "
                    + "run the script again, or raise \"Action timeout\" in the NeoHouse settings.").formatted(this.listDescription, timeoutTicks));
        return false;
    }

    private boolean sameApartFromClickedSlot(List<String> page) {
        if (page.size() != this.pageBefore.size()) return false;
        for (int i = 0; i < page.size(); i++)
            if (i != this.clickedPosition && !page.get(i).equals(this.pageBefore.get(i))) return false;
        return true;
    }

    /** Ticks the server took to answer the click, once {@link #hasTurned} returned true. */
    int responseTicks() {
        return this.waitedTicks;
    }
}
