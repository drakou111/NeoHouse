package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class WaitForAction extends Action {
    private final Class<? extends Screen> screenClass;
    private final boolean waitForOpen;
    private boolean done = false;
    private boolean registered = false;

    public WaitForAction(Class<? extends Screen> screenClass, boolean waitForOpen) {
        this.screenClass = screenClass;
        this.waitForOpen = waitForOpen;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!registered) {
            ScreenEvents.watch(this);
            registered = true;
        }
        return done;
    }

    public boolean matchesScreen(Screen screen, boolean isOpening) {
        return isOpening == waitForOpen && screen != null && screenClass.isAssignableFrom(screen.getClass());
    }

    public void markDone() {
        this.done = true;
    }

    public void reset() {
        this.done = false;
    }
}
