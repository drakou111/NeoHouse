package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;

public class SequenceManager {
    private static Action currentAction = null;
    private static long startTimeMs = 0;
    private static int totalSteps = 0;
    private static int completedSteps = 0;

    public static void startAction(Action action) {
        currentAction = action;
        startTimeMs = System.currentTimeMillis();
        totalSteps = action.countSteps();
        completedSteps = 0;
    }

    public static void cancelAction() {
        currentAction = null;
    }

    public static boolean isRunning() {
        return currentAction != null;
    }

    public static void stepCompleted() {
        completedSteps++;
    }

    public static long getElapsedMs() { return System.currentTimeMillis() - startTimeMs; }
    public static int getTotalSteps()     { return totalSteps; }
    public static int getCompletedSteps() { return completedSteps; }

    public static void tick(Minecraft mc) {
        if (currentAction == null) return;

        if (!HousingCheck.isInHousing()) {
            MessageUtils.errorToChat("Script stopped: no longer in Housing.");
            currentAction = null;
            return;
        }

        if (mc.player != null && !mc.player.getAbilities().instabuild && currentAction.requiresCreative()) {
            MessageUtils.errorToChat("Script stopped: creative mode is required for item actions.");
            currentAction = null;
            return;
        }

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
