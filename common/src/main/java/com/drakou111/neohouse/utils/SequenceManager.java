package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;

public class SequenceManager {
    private static Action currentAction = null;

    public static void startAction(Action action) {
        currentAction = action;
    }

    public static void cancelAction() {
        currentAction = null;
    }

    public static void tick(Minecraft mc) {
        if (currentAction == null) return;

        boolean finished;
        try {
            finished = currentAction.run(mc);
        } catch (InterruptActionException e) {
            MessageUtils.errorToChat(e.getMessage() != null ? e.getMessage() : e.toString());
            currentAction = null;
            return;
        } catch (Exception e) {
            MessageUtils.errorToChat("Script aborted due to unexpected error: " + e);
            currentAction = null;
            return;
        }

        if (finished) {
            MessageUtils.debugToChat("Done!");
            currentAction = null;
        }
    }
}
