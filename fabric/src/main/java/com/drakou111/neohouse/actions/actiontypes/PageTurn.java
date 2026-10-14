package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.exceptions.InterruptActionException;

import java.util.List;

/**
 * Waits for a clicked next-page arrow to swap the page in. The arrow was only clicked because it
 * said there is a next page, so a page that never changes means lag, not "last page": guessing
 * there would make a search miss items that exist, so it fails with a clear error instead.
 */
final class PageTurn {
    private final String listDescription;
    private List<String> pageBefore = null;
    private int waitedTicks = 0;

    PageTurn(String listDescription) {
        this.listDescription = listDescription;
    }

    void start(List<String> pageBefore) {
        this.pageBefore = pageBefore;
        this.waitedTicks = 0;
    }

    /** Call once per tick with the current page; true once it differs from the page before the click. */
    boolean hasTurned(List<String> page, int timeoutTicks) {
        if (!page.equals(this.pageBefore)) return true;
        if (++this.waitedTicks > timeoutTicks)
            throw new InterruptActionException(("The next page of the %s didn't load within %d ticks. The server may be lagging: "
                    + "run the script again, or raise \"Action timeout\" in the NeoHouse settings.").formatted(this.listDescription, timeoutTicks));
        return false;
    }

    /** Ticks the server took to answer the click, once {@link #hasTurned} returned true. */
    int responseTicks() {
        return this.waitedTicks;
    }
}
