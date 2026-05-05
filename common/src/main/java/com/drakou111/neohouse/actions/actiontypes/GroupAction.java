package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

import java.util.List;

public class GroupAction extends Action {
    private final List<Action> actions;
    private int index = 0;

    public GroupAction(List<Action> actions) {
        this.actions = actions;
    }

    @Override
    public boolean run(Minecraft mc) {
        while (index < actions.size()) {
            if (!actions.get(index).run(mc)) return false;
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
}