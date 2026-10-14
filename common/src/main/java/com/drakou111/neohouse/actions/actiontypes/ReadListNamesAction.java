package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages through an already-open Housing list (functions, commands or custom menus) and records every
 * entry name in {@link SequenceManager}, so later steps can decide what to delete, create or skip
 * without searching the list again.
 *
 * <p>Entries live in rows 1-3, columns 1-7 (slots 10-16, 19-25, 28-34). The last slot (53) is the
 * next-page arrow. Paging stops when a page adds no new name (last page, or an arrow that didn't advance).</p>
 */
public class ReadListNamesAction extends Action {

    private static final int[] ENTRY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int MAX_PAGES = 1000;
    private static final int SETTLE_TIMEOUT = 40; // ticks to wait for the next page to load
    private static final int FILL_TICKS = 5;      // grace ticks so a half-filled page isn't read
    private static final int PRE_CLICK_TICKS = 5; // pause before clicking to the next page

    private final HousingList list;
    private List<String> lastPage = null;
    private int pages = 0;
    private boolean paging = false;
    private int settleTicks = 0;
    private int waitTicks = FILL_TICKS; // also covers the list having just been opened
    private boolean pendingClick = false;

    public ReadListNamesAction(HousingList list) {
        this.list = list;
    }

    @Override
    public String getDebugName() {
        return "Read " + this.list.name().toLowerCase() + " list";
    }

    private static List<String> readPage(AbstractContainerMenu menu) {
        List<String> page = new ArrayList<>();
        for (int slot : ENTRY_SLOTS) {
            if (slot < menu.slots.size() && menu.slots.get(slot).hasItem())
                page.add(menu.slots.get(slot).getItem().getHoverName().getString());
        }
        return page;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (this.waitTicks > 0) {
            this.waitTicks--;
            return false;
        }

        if (this.pendingClick) {
            this.pendingClick = false;
            if (NEXT_PAGE_SLOT < menu.slots.size() && menu.slots.get(NEXT_PAGE_SLOT).hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, NEXT_PAGE_SLOT, 0, ClickType.PICKUP, mc.player);
                this.paging = true;
                return false;
            }
            return true;
        }

        List<String> page = readPage(menu);

        if (this.paging) {
            // Wait for the clicked arrow to swap the page contents in before reading it.
            if (page.equals(this.lastPage) && this.settleTicks < SETTLE_TIMEOUT) {
                this.settleTicks++;
                return false;
            }
            this.paging = false;
            this.settleTicks = 0;
            this.waitTicks = FILL_TICKS;
            return false;
        }

        boolean addedNew = false;
        for (String name : page) addedNew |= SequenceManager.recordExisting(this.list, name);
        if (!addedNew || this.pages >= MAX_PAGES) return true;

        this.lastPage = page;
        this.pages++;
        this.pendingClick = true;
        this.waitTicks = PRE_CLICK_TICKS;
        return false;
    }
}
