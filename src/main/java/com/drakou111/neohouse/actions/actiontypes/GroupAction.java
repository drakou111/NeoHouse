package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

import java.util.Iterator;
import java.util.List;

public class GroupAction extends Action {
    private final List<Action> actions;
    private final Iterator<Action> it;
    private Action current;

    public GroupAction(List<Action> actions) {
        this.actions = actions;
        this.it = actions.iterator();
        this.current = it.hasNext() ? it.next() : null;
    }

    @Override
    public boolean requiresCreative() {
        return actions.stream().anyMatch(Action::requiresCreative);
    }

    @Override
    public boolean run(Minecraft mc) {
        while (current != null) {
            if (!current.run(mc)) return false;
            current = it.hasNext() ? it.next() : null;
        }
        return true;
    }
}
