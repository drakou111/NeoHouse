package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.drakou111.neohouse.platform.Platforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class WaitForAction extends Action {
    private static final int TIMEOUT_TICKS = 200;

    private final Class<? extends Screen> screenClass;
    private final boolean waitForOpen;
    private boolean done = false;
    private boolean registered = false;
    private int ticksWaited = 0;

    public WaitForAction(Class<? extends Screen> screenClass, boolean waitForOpen) {
        this.screenClass = screenClass;
        this.waitForOpen = waitForOpen;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!registered) {
            Platforms.get().watchForScreen(this);
            registered = true;
        }
        if (done) return true;
        if (++ticksWaited > TIMEOUT_TICKS) {
            throw new InterruptActionException("Timed out waiting for UI screen to " + (waitForOpen ? "open" : "close") + ".");
        }
        return false;
    }

    public boolean matchesScreen(Screen screen, boolean isOpening) {
        return isOpening == waitForOpen && screen != null && screenClass.isAssignableFrom(screen.getClass());
    }

    public void markDone() {
        this.done = true;
    }

    public void reset() {
        this.done = false;
        this.registered = false;
        this.ticksWaited = 0;
    }
}
