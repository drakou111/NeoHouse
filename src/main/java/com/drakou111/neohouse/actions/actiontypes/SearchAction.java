package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InteruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

/**
 * Search primary slots for itemName; if missing, click backupSlot,
 * wait for UI change, then re‑search primary slots one more time.
 */
public class SearchAction extends Action {
    private final int[] primarySlots;
    private final String targetName;
    private final Item itemType;
    private final int backupSlot;
    private final WaitForAction waitFor;
    private final WaitTicksAction waitTicks;

    private int step = 0;

    public SearchAction(int[] primarySlots, String targetName, Item itemType, int backupSlot) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.itemType = itemType;
        this.backupSlot = backupSlot;
        this.waitFor = new WaitForAction(ContainerScreen.class, true);
        this.waitTicks = new WaitTicksAction(1);
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        // STEP 0 & 2: search primary slots
        if (step == 0) {
            for (int slotIndex : primarySlots) {
                Slot slot = menu.getSlot(slotIndex);
                if (slot.hasItem() && slot.getItem().getHoverName().getString().equals(targetName) && (itemType == null || slot.getItem().getItem() == itemType) && mc.gameMode != null) {
                    mc.gameMode.handleInventoryMouseClick(
                            menu.containerId, slotIndex, 0, ClickType.PICKUP, mc.player
                    );
                    return true;
                }
            }
            step = 1;
        }

        // STEP 1: click backup arrow
        if (step == 1) {
            Slot back = menu.getSlot(backupSlot);
            if (back.hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        menu.containerId, backupSlot, 0, ClickType.PICKUP, mc.player
                );
                // now wait for UI update
                step = 3;
                return false;
            } else {
                // no arrow to click → was not found
                throw new InteruptActionException("Couldn't find the item named '" + targetName + "'.");
            }
        }

        // STEP 3: wait for screen change
        if (step == 3) {
            if (waitFor.run(mc) && waitTicks.run(mc)) {
                step = 0;
                waitFor.reset();
                waitTicks.reset();
            }
            return false;
        }

        throw new InteruptActionException("Couldn't find the item named '" + targetName + "'.");
    }
}
