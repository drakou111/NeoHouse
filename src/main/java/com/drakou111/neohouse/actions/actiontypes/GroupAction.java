package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.MessageUtils;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
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
    public ActionResult run(Minecraft mc) {
        if (current == null) return new ActionResult(true);
        ActionResult result = current.run(mc);

        if (result.hasError()) {
            if (!result.hasAbort())
                MessageUtils.errorToChat(result.error);
            return new ActionResult(true, result.error, true);
        }

        if (result.finished) this.current = it.hasNext() ? it.next() : null;

        boolean finished = (current == null);
        return new ActionResult(finished);
    }
}
