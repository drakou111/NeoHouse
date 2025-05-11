package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Weather {
    SUNNY(new PageSlot(0, 10)),
    RAINING(new PageSlot(0, 11));

    public final PageSlot pageSlot;

    Weather(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
