package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Gamemode {
    ADVENTURE(new PageSlot(0, 10)),
    SURVIVAL(new PageSlot(0, 11)),
    CREATIVE(new PageSlot(0, 12));

    public final PageSlot pageSlot;

    Gamemode(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
