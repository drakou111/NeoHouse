package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.actions.actiontypes.WaitForChatAction;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;

import java.util.LinkedHashSet;
import java.util.Set;

public class ChatEvents {
    private static final Set<WaitForChatAction> waitingActions = new LinkedHashSet<>();

    public static void watch(WaitForChatAction action) {
        waitingActions.add(action);
    }

    /** Called by platform-specific event handlers when a game chat message arrives. */
    public static void onChatMessage(String plainText) {
        for (WaitForChatAction action : waitingActions) {
            if (action.matches(plainText)) {
                action.markMatched();
            }
        }
        if (SequenceManager.isRunning() && plainText.contains("Team not found!")) {
            SequenceManager.cancelAction();
            MessageUtils.errorToChat("Script stopped: Team not found.");
        }
    }
}
