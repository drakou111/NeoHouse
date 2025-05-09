package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.PageSlot;

public enum InventorySlots {
    HELMET(new PageSlot(0, 1)),
    CHEST_PLATE(new PageSlot(0, 2)),
    LEGGINGS(new PageSlot(0, 3)),
    BOOTS(new PageSlot(0, 4)),
    FIRST_AVAILABLE_SLOT(new PageSlot(0, 6)),
    HAND_SLOT(new PageSlot(0, 7)),
    MANUAL_INPUT(new PageSlot(0, 8)),
    SLOT_HOTBAR_0(new PageSlot(0, 36)),
    SLOT_HOTBAR_1(new PageSlot(0, 37)),
    SLOT_HOTBAR_2(new PageSlot(0, 38)),
    SLOT_HOTBAR_3(new PageSlot(0, 39)),
    SLOT_HOTBAR_4(new PageSlot(0, 40)),
    SLOT_HOTBAR_5(new PageSlot(0, 41)),
    SLOT_HOTBAR_6(new PageSlot(0, 42)),
    SLOT_HOTBAR_7(new PageSlot(0, 43)),
    SLOT_HOTBAR_8(new PageSlot(0, 44)),
    SLOT_9(new PageSlot(0, 9)),
    SLOT_10(new PageSlot(0, 10)),
    SLOT_11(new PageSlot(0, 11)),
    SLOT_12(new PageSlot(0, 12)),
    SLOT_13(new PageSlot(0, 13)),
    SLOT_14(new PageSlot(0, 14)),
    SLOT_15(new PageSlot(0, 15)),
    SLOT_16(new PageSlot(0, 16)),
    SLOT_17(new PageSlot(0, 17)),
    SLOT_18(new PageSlot(0, 18)),
    SLOT_19(new PageSlot(0, 19)),
    SLOT_20(new PageSlot(0, 20)),
    SLOT_21(new PageSlot(0, 21)),
    SLOT_22(new PageSlot(0, 22)),
    SLOT_23(new PageSlot(0, 23)),
    SLOT_24(new PageSlot(0, 24)),
    SLOT_25(new PageSlot(0, 25)),
    SLOT_26(new PageSlot(0, 26)),
    SLOT_27(new PageSlot(0, 27)),
    SLOT_28(new PageSlot(0, 28)),
    SLOT_29(new PageSlot(0, 29)),
    SLOT_30(new PageSlot(0, 30)),
    SLOT_31(new PageSlot(0, 31)),
    SLOT_32(new PageSlot(0, 32)),
    SLOT_33(new PageSlot(0, 33)),
    SLOT_34(new PageSlot(0, 34)),
    SLOT_35(new PageSlot(0, 35));

    public final PageSlot pageSlot;

    InventorySlots(PageSlot pageSlot) {
        this.pageSlot = pageSlot;
    }

    public int getPage() {
        return this.pageSlot.page;
    }

    public int getSlotID() {
        return this.pageSlot.slotID;
    }
}
