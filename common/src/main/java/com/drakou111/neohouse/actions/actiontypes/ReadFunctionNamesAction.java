package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages through the already-open Functions list and records every function name into
 * {@link SequenceManager}'s existing-function set, so Phase 1 can skip delete-searches for
 * functions that aren't there.
 *
 * <p>Function items live in rows 1-3, columns 1-7 (slots 10-16, 19-25, 28-34). The last slot (53)
 * is the next-page arrow. Paging stops when a page is empty or repeats (the arrow didn't advance).</p>
 */
public class ReadFunctionNamesAction extends Action {

    private static final int[] FN_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MAX_PAGES      = 1000;
    private static final int SETTLE_TIMEOUT = 40; // ticks to wait for the next page to load
    private static final int FILL_TICKS     = 5;  // grace ticks so a half-filled page isn't read
    private static final int PRE_CLICK_TICKS = 5; // pause before clicking to the next page

    private List<String> lastPage = null;
    private int pages       = 0;
    private boolean paging  = false;
    private int settleTicks = 0;
    private int waitTicks   = FILL_TICKS; // also covers the menu having just been opened
    private boolean pendingClick = false;

    private static List<String> readPage(AbstractContainerMenu menu) {
        List<String> page = new ArrayList<>();
        for (int slot : FN_SLOTS) {
            if (slot < menu.slots.size() && menu.slots.get(slot).hasItem()) {
                page.add(menu.slots.get(slot).getItem().getHoverName().getString());
            }
        }
        return page;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (waitTicks > 0) { waitTicks--; return false; }

        if (pendingClick) {
            pendingClick = false;
            if (NEXT_PAGE_SLOT < menu.slots.size() && menu.slots.get(NEXT_PAGE_SLOT).hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, NEXT_PAGE_SLOT, 0, ClickType.PICKUP, mc.player);
                paging = true;
                return false;
            }
            return true;
        }

        List<String> page = readPage(menu);

        if (paging) {
            // Wait for the clicked arrow to swap the page contents in before reading it.
            if (page.equals(lastPage) && settleTicks < SETTLE_TIMEOUT) { settleTicks++; return false; }
            paging = false;
            settleTicks = 0;
            // Contents just started swapping in; let the rest of the page arrive before reading it.
            waitTicks = FILL_TICKS;
            return false;
        }

        // Record names; stop as soon as a page adds nothing new. This covers the last page, an
        // arrow that does nothing, and an arrow that wraps back to an already-seen page.
        boolean addedNew = false;
        for (String name : page) {
            if (!SequenceManager.functionExists(name)) {
                SequenceManager.recordExistingFunction(name);
                addedNew = true;
            }
        }
        if (!addedNew || pages >= MAX_PAGES) return true;

        lastPage = page;
        pages++;

        // Don't hammer the arrow: pause a moment, then click on a later tick.
        pendingClick = true;
        waitTicks = PRE_CLICK_TICKS;
        return false;
    }

    @Override
    public int countSteps() { return 1; }
}
