package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum Enchant {
    PROTECTION(new PageSlot(0, 10)),
    FIRE_PROTECTION(new PageSlot(0, 11)),
    FEATHER_FALLING(new PageSlot(0, 12)),
    BLAST_PROTECTION(new PageSlot(0, 13)),
    PROJECTILE_PROTECTION(new PageSlot(0, 14)),
    RESPIRATION(new PageSlot(0, 15)),
    AQUA_AFFINITY(new PageSlot(0, 16)),
    THORNS(new PageSlot(0, 19)),
    DEPTH_STRIDER(new PageSlot(0, 20)),
    SHAPRNESS(new PageSlot(0, 21)),
    SMITE(new PageSlot(0, 22)),
    BANE_OF_ARTHROPODS(new PageSlot(0, 23)),
    KNOCKBACK(new PageSlot(0, 24)),
    FIRE_ASPECT(new PageSlot(0, 25)),
    LOOTING(new PageSlot(0, 28)),
    EFFICIENCY(new PageSlot(0, 29)),
    SILK_TOUCH(new PageSlot(0, 30)),
    UNBREAKING(new PageSlot(0, 31)),
    FORTUNE(new PageSlot(0, 32)),
    POWER(new PageSlot(0, 33)),
    PUNCH(new PageSlot(0, 34)),

    FLAME(new PageSlot(1, 10)),
    INFINITY(new PageSlot(1, 11)),
    LUCK_OF_THE_SEA(new PageSlot(1, 12)),
    LURE(new PageSlot(1, 13));

    public final PageSlot pageSlot;

    Enchant(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
