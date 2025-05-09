package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

import java.util.Iterator;
import java.util.List;

public class GroupAction extends Action {
    private final Iterator<Action> it;
    private Action current;

    public GroupAction(List<Action> actions) {
        this.it = actions.iterator();
        this.current = it.hasNext() ? it.next() : null;
    }

    /**
     * Drives the sequence one step.
     * @return an ActionResult whose `finished` flag is true when the entire sequence is done.
     */
    @Override
    public boolean run(Minecraft mc) {
        if (current == null) return true;
        boolean finished = current.run(mc);

        if (finished) this.current = it.hasNext() ? it.next() : null;

        return (current == null);
    }
}
