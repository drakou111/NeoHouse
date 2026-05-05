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

    public static void onScreenOpened(Screen screen) {
        for (WaitForAction action : waitingActions) {
            if (action.matchesScreen(screen, true)) action.markDone();
        }
    }

    public static void onScreenClosed(Screen screen) {
        for (WaitForAction action : waitingActions) {
            if (action.matchesScreen(screen, false)) action.markDone();
        }
    }
}
