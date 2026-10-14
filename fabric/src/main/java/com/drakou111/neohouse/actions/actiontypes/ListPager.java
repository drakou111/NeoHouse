package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.compat.ContainerClicks;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Walks the pages of an open paginated Housing menu (a list of functions, commands, menus or
 * actions): each page is handed to a {@link PageReader} once it has finished loading (see
 * {@link MenuSettling}), then the next-page arrow is clicked until there is none. A page that never
 * turns after clicking the arrow is an error, not the end of the list.
 *
 * <p>Entries live in rows 1-3, columns 1-7 (slots 10-16, 19-25, 28-34). The last slot (53) is the
 * next-page arrow.</p>
 */
final class ListPager {
    private static final int[] ENTRY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MAX_PAGES = 1000;
    private static final int STABLE_TICKS = 4;

    /** Reads one loaded page; returns false to stop before turning to the next one. */
    @FunctionalInterface
    interface PageReader {
        boolean read(List<String> entryNames);
    }

    private enum Step { SETTLE, TURN_PAGE }

    private final String description;
    private final boolean paginated;
    private final Predicate<AbstractContainerMenu> isLoaded;
    private final MenuSettling settling = new MenuSettling(STABLE_TICKS);
    private final PageTurn pageTurn;
    private Step step = Step.SETTLE;
    private boolean started = false;
    private int pages = 0;
    private int stepTicks = 0;

    /** {@code isLoaded} tells a loaded page from one still arriving (an empty list loads no entry). */
    ListPager(String description, boolean paginated, Predicate<AbstractContainerMenu> isLoaded) {
        this.description = description;
        this.paginated = paginated;
        this.isLoaded = isLoaded;
        this.pageTurn = new PageTurn(description);
    }

    /** Call once per tick; true once the last page has been read. */
    boolean run(Minecraft mc, PageReader reader) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        int timeout = NeoHouseConfig.get().actionTimeoutTicks;
        if (!this.started) {
            this.started = true;
            this.settling.expectLag(MenuSettling.pingTicks(mc));
        }

        if (this.step == Step.TURN_PAGE) {
            List<String> now = MenuSettling.isOpenMenu(mc, menu) ? MenuSettling.snapshot(menu) : List.of();
            if (!this.pageTurn.hasTurned(now, timeout)) return false;
            startSettling(Math.max(MenuSettling.pingTicks(mc), this.pageTurn.responseTicks()));
            return false;
        }

        if (++this.stepTicks > timeout)
            throw new InterruptActionException("Timed out waiting for the " + this.description + " to load.");
        if (!MenuSettling.isOpenMenu(mc, menu) || !this.isLoaded.test(menu)) {
            // Closed or between menus (a laggy reopen): wait for the list to come back.
            this.settling.reset();
            return false;
        }
        List<String> snapshot = MenuSettling.snapshot(menu);
        if (!this.settling.update(snapshot)) return false;

        if (!reader.read(readEntries(menu)) || ++this.pages >= MAX_PAGES) return true;

        boolean hasNextPage = this.paginated && NEXT_PAGE_SLOT < menu.slots.size()
                && MenuSettling.isNextPageArrow(menu.slots.get(NEXT_PAGE_SLOT).getItem());
        if (!hasNextPage || mc.gameMode == null) return true;
        ContainerClicks.pickup(mc, menu.containerId, NEXT_PAGE_SLOT, 0);
        // MenuSettling.snapshot lists the menu's own slots in order, so slot 53 is position 53.
        this.pageTurn.start(snapshot, NEXT_PAGE_SLOT);
        this.step = Step.TURN_PAGE;
        return false;
    }

    private void startSettling(int expectedLagTicks) {
        this.settling.reset();
        this.settling.expectLag(expectedLagTicks);
        this.stepTicks = 0;
        this.step = Step.SETTLE;
    }

    private static List<String> readEntries(AbstractContainerMenu menu) {
        List<String> page = new ArrayList<>();
        for (int slot : ENTRY_SLOTS) {
            if (slot < menu.slots.size() && menu.slots.get(slot).hasItem())
                page.add(MenuSettling.plainName(menu.slots.get(slot).getItem()));
        }
        return page;
    }
}
