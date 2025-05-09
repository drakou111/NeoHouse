package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum AmountCheck {
    ANY(new PageSlot(0, 10)),
    GREATER_THAN_OR_EQUAL(new PageSlot(0, 11));

    public final PageSlot pageSlot;

    AmountCheck(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
