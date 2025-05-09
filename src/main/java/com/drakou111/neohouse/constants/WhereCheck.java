package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum WhereCheck {
    HAND(new PageSlot(0, 10)),
    ARMOR(new PageSlot(0, 11)),
    HOTBAR(new PageSlot(0, 12)),
    INVENTORY(new PageSlot(0, 13)),
    CURSOR(new PageSlot(0, 14)),
    CRAFTING_GRID(new PageSlot(0, 15)),
    ANYWHERE(new PageSlot(0, 16));

    public final PageSlot pageSlot;

    WhereCheck(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
