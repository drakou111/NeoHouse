package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Holder {
    PLAYER(new PageSlot(0, 10)),
    GLOBAL(new PageSlot(0, 11)),
    TEAM(new PageSlot(0, 12));

    public final PageSlot pageSlot;

    Holder(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
