package com.drakou111.neohouse.constants;

public enum WhereCheck {
    HAND("Hand"),
    ARMOR("Armor"),
    HOTBAR("Hotbar"),
    INVENTORY("Inventory"),
    CURSOR("Cursor"),
    CRAFTING_GRID("Crafting Grid"),
    ANYWHERE("Anywhere");

    public final String displayName;

    WhereCheck(String displayName) {
        this.displayName = displayName;
    }
}
