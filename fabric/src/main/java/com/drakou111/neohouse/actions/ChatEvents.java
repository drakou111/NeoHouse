package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.actions.actiontypes.WaitForChatAction;
import com.drakou111.neohouse.utils.SequenceManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatEvents {
    private static final Set<WaitForChatAction> waitingActions = new CopyOnWriteArraySet<>();

    // Server messages that should immediately abort a running script, mapped to the
    // reason shown to the user.
    private static final Map<String, String> STOP_TRIGGERS = Map.of(
            "Team not found!", "Team not found.",
            "You do not have permission", "No permission."
    );

    public static void watch(WaitForChatAction action) {
        waitingActions.add(action);
    }

    public static void unwatch(WaitForChatAction action) {
        waitingActions.remove(action);
    }

    public static void clear() {
        waitingActions.clear();
    }

    public static void onChatMessage(String plainText) {
        for (WaitForChatAction action : waitingActions) {
            if (action.matches(plainText)) {
                action.markMatched();
            }
        }
        if (!SequenceManager.isRunning()) return;
        for (Map.Entry<String, String> trigger : STOP_TRIGGERS.entrySet()) {
            if (plainText.contains(trigger.getKey())) {
                SequenceManager.stop("Script stopped: " + trigger.getValue(), true);
                return;
            }
        }
    }
}
