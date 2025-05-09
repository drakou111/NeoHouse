package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;

public class SlotAction extends Action {
    private final int slotIndex;

    public SlotAction(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public ActionResult run(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu != null && Minecraft.getInstance().gameMode != null) {
            if (mc.player.containerMenu.slots.size() <= slotIndex) return new ActionResult(true, "Tried clicking a slot that doesn't exist." ,false);
            
            Minecraft.getInstance().gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotIndex, 0, ClickType.PICKUP, mc.player);
            return new ActionResult(true);
        }

        return new ActionResult(true, "An unexpected error occurred, could not click a slot.", false);
    }
}