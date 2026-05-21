package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.NeoHouseConfig;
import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class WaitForAction extends Action {

    private final Class<? extends Screen> screenClass;
    private final boolean waitForOpen;
    private Screen screenAtStart = null;
    private boolean initialized = false;
    private int ticksWaited = 0;

    public WaitForAction(Class<? extends Screen> screenClass, boolean waitForOpen) {
        this.screenClass = screenClass;
        this.waitForOpen = waitForOpen;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!initialized) {
            screenAtStart = mc.screen;
            initialized = true;
        }
        if (++ticksWaited > NeoHouseConfig.get().actionTimeoutTicks) {
            throw new InterruptActionException("Timed out waiting for screen to " + (waitForOpen ? "open" : "close") + ".");
        }
        Screen current = mc.screen;
        if (waitForOpen) {
            return current != null && current != screenAtStart && screenClass.isAssignableFrom(current.getClass());
        } else {
            return current == null || !screenClass.isAssignableFrom(current.getClass());
        }
    }

    @Override
    public String getDebugName() {
        return "Wait for screen to " + (waitForOpen ? "open" : "close") + ": " + screenClass.getSimpleName();
    }

    public void reset() {
        this.initialized = false;
        this.screenAtStart = null;
        this.ticksWaited = 0;
    }
}
