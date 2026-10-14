package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import net.minecraft.client.Minecraft;

public class EmptyAction extends Action {
    @Override
    public boolean run(Minecraft mc) {
        return true;
    }

    @Override
    public int countSteps() { return 0; }

    @Override
    public boolean isContainer() { return true; }
}
