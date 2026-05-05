package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.utils.PageSlot;

public enum InventorySlots {
    HELMET              (new PageSlot(0, 1),  "Helmet"),
    CHEST_PLATE         (new PageSlot(0, 2),  "Chestplate"),
    LEGGINGS            (new PageSlot(0, 3),  "Leggings"),
    BOOTS               (new PageSlot(0, 4),  "Boots"),
    FIRST_AVAILABLE_SLOT(new PageSlot(0, 6),  "First Available Slot"),
    HAND_SLOT           (new PageSlot(0, 7),  "Hand Slot"),
    // MANUAL_INPUT     (new PageSlot(0, 8),  null),   // slower than search-based slots; kept out for now
    HOTBAR_0            (new PageSlot(0, 36), "Hotbar Slot 1"),
    HOTBAR_1            (new PageSlot(0, 37), "Hotbar Slot 2"),
    HOTBAR_2            (new PageSlot(0, 38), "Hotbar Slot 3"),
    HOTBAR_3            (new PageSlot(0, 39), "Hotbar Slot 4"),
    HOTBAR_4            (new PageSlot(0, 40), "Hotbar Slot 5"),
    HOTBAR_5            (new PageSlot(0, 41), "Hotbar Slot 6"),
    HOTBAR_6            (new PageSlot(0, 42), "Hotbar Slot 7"),
    HOTBAR_7            (new PageSlot(0, 43), "Hotbar Slot 8"),
    HOTBAR_8            (new PageSlot(0, 44), "Hotbar Slot 9"),
    SLOT_9              (new PageSlot(0, 9),  "Inventory Slot 1"),
    SLOT_10             (new PageSlot(0, 10), "Inventory Slot 2"),
    SLOT_11             (new PageSlot(0, 11), "Inventory Slot 3"),
    SLOT_12             (new PageSlot(0, 12), "Inventory Slot 4"),
    SLOT_13             (new PageSlot(0, 13), "Inventory Slot 5"),
    SLOT_14             (new PageSlot(0, 14), "Inventory Slot 6"),
    SLOT_15             (new PageSlot(0, 15), "Inventory Slot 7"),
    SLOT_16             (new PageSlot(0, 16), "Inventory Slot 8"),
    SLOT_17             (new PageSlot(0, 17), "Inventory Slot 9"),
    SLOT_18             (new PageSlot(0, 18), "Inventory Slot 10"),
    SLOT_19             (new PageSlot(0, 19), "Inventory Slot 11"),
    SLOT_20             (new PageSlot(0, 20), "Inventory Slot 12"),
    SLOT_21             (new PageSlot(0, 21), "Inventory Slot 13"),
    SLOT_22             (new PageSlot(0, 22), "Inventory Slot 14"),
    SLOT_23             (new PageSlot(0, 23), "Inventory Slot 15"),
    SLOT_24             (new PageSlot(0, 24), "Inventory Slot 16"),
    SLOT_25             (new PageSlot(0, 25), "Inventory Slot 17"),
    SLOT_26             (new PageSlot(0, 26), "Inventory Slot 18"),
    SLOT_27             (new PageSlot(0, 27), "Inventory Slot 19"),
    SLOT_28             (new PageSlot(0, 28), "Inventory Slot 20"),
    SLOT_29             (new PageSlot(0, 29), "Inventory Slot 21"),
    SLOT_30             (new PageSlot(0, 30), "Inventory Slot 22"),
    SLOT_31             (new PageSlot(0, 31), "Inventory Slot 23"),
    SLOT_32             (new PageSlot(0, 32), "Inventory Slot 24"),
    SLOT_33             (new PageSlot(0, 33), "Inventory Slot 25"),
    SLOT_34             (new PageSlot(0, 34), "Inventory Slot 26"),
    SLOT_35             (new PageSlot(0, 35), "Inventory Slot 27");

    public final PageSlot pageSlot;
    public final String displayName;

    InventorySlots(PageSlot pageSlot, String displayName) {
        this.pageSlot    = pageSlot;
        this.displayName = displayName;
    }

    public int getPage()   { return this.pageSlot.page; }
    public int getSlotID() { return this.pageSlot.slotID; }
}
