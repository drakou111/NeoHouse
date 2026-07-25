package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

/**
 * Runs the wrapped action only if the named function/loop was created as a fresh shell earlier
 * this run (see {@link MarkCreatedAction}). Used by "Never Override": the body is written only
 * for entities we created, so anything that already existed is left untouched.
 *
 * <p>Progress steps are counted up-front (before the run/skip decision is known); when the action
 * is skipped its steps are subtracted so the progress total stays accurate, mirroring how
 * {@link SearchAction} reconciles its branches.</p>
 */
public class RunIfCreatedAction extends Action {
    private final String name;
    private final Action action;
    private boolean decided = false;
    private Action active = null;

    public RunIfCreatedAction(String name, Action action) {
        this.name = name;
        this.action = action;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!decided) {
            decided = true;
            if (SequenceManager.wasCreated(name)) {
                active = action;
            } else {
                active = null;
                if (action != null) SequenceManager.addSteps(-action.countSteps());
            }
        }
        if (active == null) return true;
        if (active.run(mc)) { active = null; return true; }
        return false;
    }

    @Override
    public int countSteps() {
        return action != null ? action.countSteps() : 0;
    }

    @Override
    public boolean requiresCreative() {
        return action != null && action.requiresCreative();
    }

    @Override
    public boolean isContainer() {
        return true;
    }
}
