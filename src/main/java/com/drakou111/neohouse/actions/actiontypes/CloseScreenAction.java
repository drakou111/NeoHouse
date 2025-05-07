package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.State;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;

public class CloseScreenAction extends Action {
    public CloseScreenAction(State startingState, State endingState) {
        super(startingState, endingState);
    }

    @Override
    public ActionResult run(Minecraft mc, State current) {
        if (mc.screen != null) mc.setScreen(null);
        return new ActionResult(endingState != null ? endingState : current, true);
    }
}

