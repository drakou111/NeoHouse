package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.MessageUtils;
import com.drakou111.neohouse.State;
import net.minecraft.client.Minecraft;
import java.util.Iterator;
import java.util.List;

public class ActionSequence {
    private final Iterator<Action> it;
    private Action current;
    private State state;

    public ActionSequence(List<Action> actions) {
        this.it = actions.iterator();
        this.current = it.hasNext() ? it.next() : null;
        this.state = State.NONE;
    }

    /** Call once per tick. Returns false when the entire sequence is finished. */
    public boolean tick(Minecraft mc) {
        if (current == null) return false;

        ActionResult result = current.run(mc, state);
        if (result == null) {
            MessageUtils.debugToChat("Something went wrong.");
            return false;
        }

        this.state = result.state;

        if (result.finished)
            this.current = it.hasNext() ? it.next() : null;

        return current != null;
    }
}
