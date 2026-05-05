package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.actions.ScreenEvents;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.neoforged.fml.ModList;

public class NeoForgePlatform implements Platform {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(c -> "v" + c.getModInfo().getVersion())
                .orElse("v?");
    }

    @Override
    public void watchForScreen(WaitForAction action) {
        ScreenEvents.watch(action);
    }
}
