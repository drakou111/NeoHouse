package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Teleport {
    HOUSE_SPAWN(new PageSlot(0, 10)),
    INVOKER(new PageSlot(0, 11)),
    CURRENT(new PageSlot(0, 12)),
    CUSTOM(new PageSlot(0, 13));

    public final PageSlot pageSlot;

    Teleport(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
