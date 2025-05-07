package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;

public class CloseScreenAction extends Action {
    public CloseScreenAction() {}

    @Override
    public ActionResult run(Minecraft mc) {
        if (mc.screen != null) mc.setScreen(null);
        return new ActionResult(true);
    }
}

