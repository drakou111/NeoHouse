package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compat.ClientUi;
import net.minecraft.client.Minecraft;

public class CloseScreenAction extends Action {
    public CloseScreenAction() {}

    @Override
    public boolean run(Minecraft mc) {
        if (ClientUi.screen(mc) != null) ClientUi.screen(mc).onClose();
        return true;
    }
}

