package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;

public class SlotAction extends Action {
    private final int slotIndex;
    private final boolean leftClick;

    public SlotAction(int slotIndex, boolean leftClick) {
        this.slotIndex = slotIndex;
        this.leftClick = leftClick;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu != null && Minecraft.getInstance().gameMode != null) {
            if (mc.player.containerMenu.slots.size() <= slotIndex) throw new InterruptActionException("Tried clicking a slot that doesn't exist.");
            
            Minecraft.getInstance().gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, leftClick ? 0 : 1, ClickType.PICKUP, mc.player);
            return true;
        }

        throw new InterruptActionException("An unexpected error occurred, could not click a slot.");
    }
}