package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compat.ContainerClicks;
import com.drakou111.neohouse.config.NeoHouseConfig;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages through an already-open Housing list (functions, commands or custom menus) and records every
 * entry name in {@link SequenceManager}, so later steps can decide what to delete, create or skip
 * without searching the list again.
 *
 * <p>Entries live in rows 1-3, columns 1-7 (slots 10-16, 19-25, 28-34). The last slot (53) is the
 * next-page arrow. Each page is read only once it has finished loading, and paging stops when a page
 * adds no new name (last page, or an arrow that didn't advance).</p>
 */
public class ReadListNamesAction extends Action {

    private static final int[] ENTRY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MAX_PAGES = 1000;
    private static final int STABLE_TICKS = 4;
    private static final int PAGE_TURN_TIMEOUT_TICKS = 60;

    private enum Step { SETTLE, TURN_PAGE }

    private final HousingList list;
    private final MenuSettling settling = new MenuSettling(STABLE_TICKS);
    private Step step = Step.SETTLE;
    private List<String> pageBeforeTurn = null;
    private int pages = 0;
    private int stepTicks = 0;

    public ReadListNamesAction(HousingList list) {
        this.list = list;
    }

    @Override
    public String getDebugName() {
        return "Read " + this.list.name().toLowerCase() + " list";
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (this.step == Step.TURN_PAGE) {
            // Wait for the clicked arrow to swap the page in; no change means it was the last page.
            if (++this.stepTicks > PAGE_TURN_TIMEOUT_TICKS) return true;
            if (MenuSettling.snapshot(menu).equals(this.pageBeforeTurn)) return false;
            startSettling();
            return false;
        }

        if (++this.stepTicks > NeoHouseConfig.get().actionTimeoutTicks)
            throw new InterruptActionException("Timed out waiting for the " + this.list.singularName + " list to load.");
        List<String> snapshot = MenuSettling.snapshot(menu);
        if (!this.settling.update(snapshot)) return false;

        boolean addedNew = false;
        for (String name : readEntries(menu)) addedNew |= SequenceManager.recordExisting(this.list, name);
        if (!addedNew || ++this.pages >= MAX_PAGES) return true;

        boolean hasNextPage = this.list.paginated && NEXT_PAGE_SLOT < menu.slots.size()
                && MenuSettling.isNextPageArrow(menu.slots.get(NEXT_PAGE_SLOT).getItem());
        if (!hasNextPage || mc.gameMode == null) return true;
        ContainerClicks.pickup(mc, menu.containerId, NEXT_PAGE_SLOT, 0);
        this.pageBeforeTurn = snapshot;
        this.stepTicks = 0;
        this.step = Step.TURN_PAGE;
        return false;
    }

    private void startSettling() {
        this.settling.reset();
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
