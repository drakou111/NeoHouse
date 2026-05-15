package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

import java.util.List;

public class GroupAction extends Action {
    private final List<Action> actions;
    private int index = 0;
    private int loggedUpTo = -1;

    public GroupAction(List<Action> actions) {
        this.actions = actions;
    }

    @Override
    public boolean run(Minecraft mc) {
        while (index < actions.size()) {
            Action child = actions.get(index);
            if (!child.isContainer() && NeoHouseConfig.get().verboseActions && index != loggedUpTo) {
                MessageUtils.debugToChat("[action] " + child.getDebugName());
                loggedUpTo = index;
            }
            if (!child.run(mc)) return false;
            if (!child.isContainer()) SequenceManager.stepCompleted();
            index++;
        }
        return true;
    }

    @Override
    public boolean requiresCreative() {
        for (Action action : actions) {
            if (action.requiresCreative()) return true;
        }
        return false;
    }

    @Override
    public int countSteps() {
        int sum = 0;
        for (Action a : actions) sum += a.countSteps();
        return sum;
    }

    @Override
    public boolean isContainer() { return true; }
}