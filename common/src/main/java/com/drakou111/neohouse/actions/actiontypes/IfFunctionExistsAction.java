package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

/**
 * Runs one of two branches depending on whether the named function already existed in Housing when
 * this run started (see {@link ReadFunctionNamesAction}). Either branch may be null (a no-op). Lets
 * Phase 1 decide delete-vs-nothing (override) and create-vs-warn (const) from the pre-read set
 * instead of re-searching the functions list per function. Steps for the branch not taken are
 * subtracted so the progress total stays accurate.
 */
public class IfFunctionExistsAction extends Action {

    private final String name;
    private final Action ifPresent;
    private final Action ifAbsent;
    private boolean decided = false;
    private Action active = null;

    public IfFunctionExistsAction(String name, Action ifPresent, Action ifAbsent) {
        this.name = name;
        this.ifPresent = ifPresent;
        this.ifAbsent = ifAbsent;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!decided) {
            decided = true;
            active = SequenceManager.functionExists(name) ? ifPresent : ifAbsent;
            int taken = active != null ? active.countSteps() : 0;
            int total = countSteps();
            if (total > taken) SequenceManager.addSteps(-(total - taken));
        }
        if (active == null) return true;
        if (active.run(mc)) { active = null; return true; }
        return false;
    }

    @Override
    public int countSteps() {
        int p = ifPresent != null ? ifPresent.countSteps() : 0;
        int a = ifAbsent  != null ? ifAbsent.countSteps()  : 0;
        return Math.max(p, a);
    }

    @Override
    public boolean requiresCreative() {
        return (ifPresent != null && ifPresent.requiresCreative())
                || (ifAbsent != null && ifAbsent.requiresCreative());
    }

    @Override
    public boolean isContainer() { return true; }
}
