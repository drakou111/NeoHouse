package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.State;
import net.minecraft.client.Minecraft;

public abstract class Action {

    protected State startingState;
    protected State endingState;

    public Action(State startingState, State endingState) {
        this.startingState = startingState;
        this.endingState = endingState;
    }

    public abstract ActionResult run(Minecraft mc, State currentState);
}
