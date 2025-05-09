package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum RestrictedOperator {
    SET(new PageSlot(0, 10)),
    INCREMENT(new PageSlot(0, 11)),
    DECREMENT(new PageSlot(0, 12)),
    MULTIPLY(new PageSlot(0, 13)),
    DIVIDE(new PageSlot(0, 14));

    public final PageSlot pageSlot;

    RestrictedOperator(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
