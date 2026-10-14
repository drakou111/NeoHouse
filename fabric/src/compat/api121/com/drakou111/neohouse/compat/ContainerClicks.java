package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;

/** Clicks a slot of the open menu the way a player would (1.21.x API). */
public final class ContainerClicks {
    private ContainerClicks() {}

    /** A plain left (button 0) or right (button 1) click on {@code slot}. */
    public static void pickup(Minecraft mc, int containerId, int slot, int button) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(containerId, slot, button, ClickType.PICKUP, mc.player);
    }
}
