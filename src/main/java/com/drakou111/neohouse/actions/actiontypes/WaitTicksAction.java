package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

public class WaitTicksAction extends Action {
    private int ticks;
    private int ticksRemaining;

    /**
     * @param ticks how many client‑ticks to wait before finishing
     */
    public WaitTicksAction(int ticks) {
        this.ticks = ticks;
        this.ticksRemaining = ticks;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (ticksRemaining <= 0) return true;
        ticksRemaining--;
        return false;
    }

    public void reset() {
        ticksRemaining = ticks;
    }
}
