package com.drakou111.neohouse.platform;

import net.minecraft.client.gui.screens.Screen;

public interface Platform {
    boolean isModLoaded(String modId);
    String getModVersion(String modId);
    String getCancelKeyName();

    /**
     * Opens a screen on the next client tick rather than immediately.
     * Required when opening from a command executor: the chat screen is closed
     * (set to null) AFTER the command runs, which would otherwise clobber a
     * directly-set screen. Deferring one tick sidesteps that race.
     */
    void openScreenNextTick(Screen screen);
}
