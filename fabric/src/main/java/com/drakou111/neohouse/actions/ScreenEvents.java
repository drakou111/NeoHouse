package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashSet;
import java.util.Set;

public class ScreenEvents {

    private static final Set<WaitForAction> waitingActions = new LinkedHashSet<>();

    public static void watch(WaitForAction action) {
        waitingActions.add(action);
    }

    public static void unwatch(WaitForAction action) {
        waitingActions.remove(action);
    }

    public static void onScreenOpened(Screen screen) {
        waitingActions.removeIf(action -> {
            if (action.matchesScreen(screen, true)) action.markDone();
            return action.isDone();
        });
    }

    public static void onScreenClosed(Screen screen) {
        waitingActions.removeIf(action -> {
            if (action.matchesScreen(screen, false)) action.markDone();
            return action.isDone();
        });
    }
}
