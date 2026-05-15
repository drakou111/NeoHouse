package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.actions.actiontypes.WaitForAction;

public interface Platform {
    boolean isModLoaded(String modId);
    String getModVersion(String modId);
    void watchForScreen(WaitForAction action);
    void unwatchForScreen(WaitForAction action);
}
