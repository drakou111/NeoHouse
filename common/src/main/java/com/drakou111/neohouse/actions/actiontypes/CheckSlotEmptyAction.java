package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Runs ifEmpty if the given slot has no item, otherwise runs ifOccupied.
 */
public class CheckSlotEmptyAction extends Action {
    private final int   slotIndex;
    private final Action ifEmpty;
    private final Action ifOccupied;
    private Action active = null;

    public CheckSlotEmptyAction(int slotIndex, Action ifEmpty, Action ifOccupied) {
        this.slotIndex   = slotIndex;
        this.ifEmpty     = ifEmpty;
        this.ifOccupied  = ifOccupied;
    }

    @Override
    public boolean requiresCreative() {
        return (ifEmpty != null && ifEmpty.requiresCreative())
                || (ifOccupied != null && ifOccupied.requiresCreative());
    }

    @Override
    public int countSteps() {
        int a = ifEmpty    != null ? ifEmpty.countSteps()    : 0;
        int b = ifOccupied != null ? ifOccupied.countSteps() : 0;
        return Math.max(a, b);
    }

    @Override
    public boolean isContainer() { return true; }

    @Override
    public boolean run(Minecraft mc) {
        if (active == null) {
            AbstractContainerMenu menu = mc.player.containerMenu;
            if (menu == null || menu.slots.size() <= slotIndex) return true;
            active = menu.slots.get(slotIndex).hasItem() ? ifOccupied : ifEmpty;
        }
        if (active.run(mc)) { active = null; return true; }
        return false;
    }
}
