package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
    private final WaitForAction waitForAction;

    private int step = 0;

    public SearchAction(int[] primarySlots, String targetName, Item itemType, int backupSlot, Class<? extends Screen> screenClass, boolean waitForOpen) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.itemType = itemType;
        this.backupSlot = backupSlot;
        this.waitForAction = new WaitForAction(screenClass, waitForOpen);
    }

    @Override
    public ActionResult run(Minecraft mc) {
        if (mc.player == null) return new ActionResult(true);
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return new ActionResult(true);

        // STEP 0 & 2: search primary slots
        if (step == 0) {
            for (int slotIndex : primarySlots) {
                Slot slot = menu.getSlot(slotIndex);
                if (slot.hasItem() && slot.getItem().getHoverName().getString().equals(targetName) && (itemType == null || slot.getItem().getItem() == itemType) && mc.gameMode != null) {
                    mc.gameMode.handleInventoryMouseClick(
                            menu.containerId, slotIndex, 0, ClickType.PICKUP, mc.player
                    );
                    return new ActionResult(true);
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
                return new ActionResult(false);
            } else {
                // no arrow to click → was not found
                return new ActionResult(true, "Couldn't find the function named '" + targetName + "'.", false);
            }
        }

        // STEP 3: wait for screen change
        if (step == 3) {
            ActionResult waitRes = waitForAction.run(mc);
            if (waitRes.finished) {
                step = 0;
                return new ActionResult(false);
            }
            return new ActionResult(false);
        }

        return new ActionResult(true, "Couldn't find the function named '" + targetName + "'.", false);
    }
}
