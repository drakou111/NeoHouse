package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum Potion {
    SPEED(new PageSlot(0, 10)),
    SLOWNESS(new PageSlot(0, 11)),
    HASTE(new PageSlot(0, 12)),
    MINING_FATIGUE(new PageSlot(0, 13)),
    STRENGTH(new PageSlot(0, 14)),
    INSTANT_HEALTH(new PageSlot(0, 15)),
    INSTANT_DAMAGE(new PageSlot(0, 16)),
    JUMP_BOOST(new PageSlot(0, 19)),
    NAUSEA(new PageSlot(0, 20)),
    REGENERATION(new PageSlot(0, 21)),
    RESISTANCE(new PageSlot(0, 22)),
    FIRE_RESISTANCE(new PageSlot(0, 23)),
    WATER_BREATHING(new PageSlot(0, 24)),
    INVISIBILITY(new PageSlot(0, 25)),
    BLINDNESS(new PageSlot(0, 28)),
    NIGHT_VISION(new PageSlot(0, 29)),
    HUNGER(new PageSlot(0, 30)),
    WEAKNESS(new PageSlot(0, 31)),
    POISON(new PageSlot(0, 32)),
    WITHER(new PageSlot(0, 33)),
    HEALTH_BOOST(new PageSlot(0, 34)),
    ABSORPTION(new PageSlot(1, 10));

    public final PageSlot pageSlot;

    Potion(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
