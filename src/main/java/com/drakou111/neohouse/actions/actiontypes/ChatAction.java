package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ChatAction extends Action {
    private final String message;

    public ChatAction(String message) {
        this.message = message;
    }

    @Override
    public ActionResult run(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player != null) {
            player.connection.sendChat(message);
            return new ActionResult(true);
        }

        return new ActionResult(true, "An unexpected error occurred, could not type in chat.", false);
    }
}