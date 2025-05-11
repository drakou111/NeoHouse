package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Time {
    RESET_WORLD_TIME(new PageSlot(0, 10)),
    SUNRISE(new PageSlot(0, 11)),
    NOON(new PageSlot(0, 12)),
    SUNSET(new PageSlot(0, 13)),
    MIDNIGHT(new PageSlot(0, 14)),
    CUSTOM(new PageSlot(0, 48));

    public final PageSlot pageSlot;

    Time(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
