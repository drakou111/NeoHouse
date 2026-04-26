package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Operator {
    SET(new PageSlot(0, 10)),
    UNSET(new PageSlot(0, 11)),
    INCREMENT(new PageSlot(0, 12)),
    DECREMENT(new PageSlot(0, 13)),
    MULTIPLY(new PageSlot(0, 14)),
    DIVIDE(new PageSlot(0, 15)),
    BITWISE_AND(new PageSlot(0, 16)),
    BITWISE_OR(new PageSlot(0, 19)),
    BITWISE_XOR(new PageSlot(0, 20)),
    LEFT_SHIFT(new PageSlot(0, 21)),
    ARITHMETIC_RIGHT_SHIFT(new PageSlot(0, 22)),
    LOGICAL_RIGHT_SHIFT(new PageSlot(0, 23));

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
