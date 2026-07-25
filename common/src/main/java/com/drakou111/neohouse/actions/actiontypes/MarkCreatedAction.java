package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;

/**
 * Records that the given function/loop was created as a fresh shell during this run, so a later
 * pass ({@link RunIfCreatedAction}) can fill only the entities we just created. Runs instantly
 * and performs no UI interaction, so it counts as zero progress steps.
 */
public class MarkCreatedAction extends Action {
    private final String name;

    public MarkCreatedAction(String name) {
        this.name = name;
    }

    @Override
    public boolean run(Minecraft mc) {
        SequenceManager.markCreated(name);
        return true;
    }

    @Override
    public int countSteps() {
        return 0;
    }
}
