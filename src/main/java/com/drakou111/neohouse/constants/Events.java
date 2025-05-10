package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Events {
    JOIN(new PageSlot(0, 10)),
    QUIT(new PageSlot(0, 11)),
    DEATH(new PageSlot(0, 12)),
    KILL(new PageSlot(0, 13)),
    RESPAWN(new PageSlot(0, 14)),
    GROUP_CHANGE(new PageSlot(0, 15)),
    PVP_STATE_CHANGE(new PageSlot(0, 16)),
    FISH_CAUGHT(new PageSlot(0, 19)),
    ENTER_PORTAL(new PageSlot(0, 20)),
    DAMAGE(new PageSlot(0, 21)),
    BLOCK_BREAK(new PageSlot(0, 22)),
    START_PARKOUR(new PageSlot(0, 23)),
    END_PARKOUR(new PageSlot(0, 24)),
    DROP_ITEM(new PageSlot(0, 25)),
    PICK_UP_ITEM(new PageSlot(0, 28)),
    CHANGE_HELD_ITEM(new PageSlot(0, 29)),
    TOGGLE_SNEAK(new PageSlot(0, 30)),
    TOGGLE_FLIGHT(new PageSlot(0, 31));

    public final PageSlot pageSlot;

    Events(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
