package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

public class CloseScreenAction extends Action {
    public CloseScreenAction() {}

    @Override
    public boolean run(Minecraft mc) {
        if (mc.screen != null) mc.screen.onClose();
        return true;
    }
}

