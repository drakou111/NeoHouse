package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.MessageUtils;
import net.minecraft.client.Minecraft;

public class ClientMessageAction extends Action {
    private final String message;
    private final boolean isWarning;

    public ClientMessageAction(String message, boolean isWarning) {
        this.message   = message;
        this.isWarning = isWarning;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (isWarning) MessageUtils.warningToChat(message);
        else           MessageUtils.debugToChat(message);
        return true;
    }
}
