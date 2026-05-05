package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;

public class SlotAction extends Action {
    private final int slotIndex;
    private final boolean leftClick;
    private final boolean fromEnd;

    public SlotAction(int slotIndex, boolean leftClick) {
        this(slotIndex, leftClick, false);
    }

    public SlotAction(int slotIndex, boolean leftClick, boolean fromEnd) {
        this.slotIndex = slotIndex;
        this.leftClick = leftClick;
        this.fromEnd = fromEnd;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc == null || mc.gameMode == null || mc.player == null) {
            throw new InterruptActionException("An unexpected error occurred, could not click a slot.");
        }

        int newIndex = fromEnd ? mc.player.containerMenu.slots.size() - 1 - slotIndex : slotIndex;
        if (mc.player.containerMenu.slots.size() <= newIndex) throw new InterruptActionException("Tried clicking a slot that doesn't exist.");
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, newIndex, leftClick ? 0 : 1, ClickType.PICKUP, mc.player);
        return true;
    }
}