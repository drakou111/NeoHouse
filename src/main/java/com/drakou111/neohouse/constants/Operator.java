package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Operator {
    UNSET(new PageSlot(0, 10)),
    SET(new PageSlot(0, 11)),
    INCREMENT(new PageSlot(0, 12)),
    DECREMENT(new PageSlot(0, 13)),
    MULTIPLY(new PageSlot(0, 14)),
    DIVIDE(new PageSlot(0, 15));

    public final PageSlot pageSlot;

    Operator(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
