package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Comparator {
    LESS_THAN(new PageSlot(0, 10)),
    LESS_THAN_OR_EQUAL(new PageSlot(0, 11)),
    EQUAL(new PageSlot(0, 12)),
    GREATER_THAN_OR_EQUAL(new PageSlot(0, 13)),
    GREATER_THAN(new PageSlot(0, 14));

    public final PageSlot pageSlot;

    Comparator(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
