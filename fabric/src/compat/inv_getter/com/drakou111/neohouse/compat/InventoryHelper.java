package com.drakou111.neohouse.compat;

import net.minecraft.world.entity.player.Player;

public final class InventoryHelper {
    private InventoryHelper() {}

    /** Returns the currently selected hotbar slot index (0-8). */
    public static int getSelectedHotbarSlot(Player player) {
        return player.getInventory().getSelectedSlot();
    }
}
