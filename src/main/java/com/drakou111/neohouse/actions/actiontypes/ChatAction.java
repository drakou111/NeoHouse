package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.State;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ChatAction extends Action {
    private final String message;

    public ChatAction(String message, State startingState, State endingState) {
        super(startingState, endingState);
        this.message = message;
    }

    @Override
    public ActionResult run(Minecraft mc, State current) {
        if (startingState != current && startingState != null) return null;

        LocalPlayer player = mc.player;
        if (player != null) {
            player.connection.sendChat(message);
            return new ActionResult(endingState, true);
        }

        return null;
    }
}