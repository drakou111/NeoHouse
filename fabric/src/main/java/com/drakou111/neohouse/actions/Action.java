package com.drakou111.neohouse.actions;

import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Action {

    public abstract boolean run(Minecraft mc);

    public boolean requiresCreative() {
        return false;
    }

    public String getDebugName() {
        return getClass().getSimpleName();
    }

    public int countSteps() {
        return 1;
    }

    public boolean isContainer() {
        return false;
    }

    /** Nested actions this action may run, for inspecting a generated action tree. */
    public List<Action> getChildren() {
        return List.of();
    }
}
