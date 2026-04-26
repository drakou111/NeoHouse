package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

public class SearchAction extends Action {
    private final int[] primarySlots;
    private final String targetName;
    private final Item itemType;
    private final int backupSlot;
    private final boolean leftClick;
    private final Action onFound;
    private final Action onNotFound;

    private final WaitForAction waitForUI = new WaitForAction(ContainerScreen.class, true);
    private final WaitTicksAction waitTick  = new WaitTicksAction(1);

    private enum Step {
        STEP_SEARCH,
        STEP_CLICK_BACKUP,
        STEP_WAIT_UI,
        STEP_WAIT_FOUND,
        STEP_FOUND,
        STEP_NOT_FOUND,
        STEP_DONE
    }

    private Step step = Step.STEP_SEARCH;

    public SearchAction(int[] primarySlots, String targetName, Item itemType, int backupSlot, boolean leftClick, Action onFound, Action onNotFound) {
        this.primarySlots = primarySlots;
        this.targetName = targetName;
        this.itemType = itemType;
        this.backupSlot = backupSlot;
        this.leftClick = leftClick;
        this.onFound = onFound;
        this.onNotFound  = onNotFound;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (step == Step.STEP_SEARCH) {
            boolean found = false;
            for (int idx : primarySlots) {
                if (idx < 0 || idx >= menu.slots.size()) continue;
                Slot s = menu.getSlot(idx);
                if (s.hasItem() && s.getItem().getHoverName().getString().equals(targetName) && (itemType == null || s.getItem().getItem() == itemType) && mc.gameMode != null) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, idx, leftClick ? 0 : 1, ClickType.PICKUP, mc.player);
                    step = Step.STEP_WAIT_FOUND;
                    found = true;
                    break;
                }
            }
            if (!found)
                step = Step.STEP_CLICK_BACKUP;
        }
        else if (step == Step.STEP_CLICK_BACKUP) {
            if (backupSlot < 0 || backupSlot >= menu.slots.size()) {
                step = Step.STEP_NOT_FOUND;
                return false;
            }
            Slot arrow = menu.getSlot(backupSlot);
            if (arrow.hasItem() && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, backupSlot, 0, ClickType.PICKUP, mc.player);
                step = Step.STEP_WAIT_UI;
            } else {
                step = Step.STEP_NOT_FOUND;
            }
        }
        else if (step == Step.STEP_WAIT_UI) {
            if (waitForUI.run(mc) && waitTick.run(mc)) {
                step = Step.STEP_SEARCH;
                waitForUI.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_WAIT_FOUND) {
            if (waitForUI.run(mc) && waitTick.run(mc)) {
                step = Step.STEP_FOUND;
                waitForUI.reset();
                waitTick.reset();
            }
        }
        else if (step == Step.STEP_FOUND) {
            if (onFound == null || onFound.run(mc)) step = Step.STEP_DONE;
        }
        else if (step == Step.STEP_NOT_FOUND) {
            if (onNotFound == null || onNotFound.run(mc)) step = Step.STEP_DONE;
            if (onNotFound == null) throw new InterruptActionException("Could not find item called '" + targetName + "'.");
        }
        else {
            return true;
        }
        return false;
    }
}
