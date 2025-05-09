package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum DamageCause {
    ENTITY_ATTACK(new PageSlot(0, 10)),
    PROJECTILE(new PageSlot(0, 11)),
    SUFFOCATION(new PageSlot(0, 12)),
    FALL(new PageSlot(0, 13)),
    LAVA(new PageSlot(0, 14)),
    FIRE(new PageSlot(0, 15)),
    FIRE_TICK(new PageSlot(0, 16)),
    DROWNING(new PageSlot(0, 19)),
    STARVATION(new PageSlot(0, 20)),
    POISON(new PageSlot(0, 21)),
    THORNS(new PageSlot(0, 22));

    public final PageSlot pageSlot;

    DamageCause(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
