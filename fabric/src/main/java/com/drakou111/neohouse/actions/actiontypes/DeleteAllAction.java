package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compat.ContainerClicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class DeleteAllAction extends Action {
    private final int slotIndex;
    private boolean waiting = false;
    private final WaitForAction waitFor;
    private final WaitTicksAction waitTicks;

    public DeleteAllAction(int slotIndex) {
        this.slotIndex = slotIndex;
        this.waitFor = new WaitForAction(ContainerScreen.class, true);
        this.waitTicks = new WaitTicksAction(1);
    }

    @Override
    public boolean run(Minecraft mc) {
        if (waiting) {
            if (!waitFor.run(mc) || !waitTicks.run(mc)) {
                return false;
            }
            waitTicks.reset();
            waitFor.reset();
            waiting = false;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;
        Slot s = menu.slots.get(slotIndex);

        if (s.getItem().isEmpty()) return true;

        ContainerClicks.pickup(mc, menu.containerId, slotIndex, 1);
        this.waiting = true;
        return false;
    }
}