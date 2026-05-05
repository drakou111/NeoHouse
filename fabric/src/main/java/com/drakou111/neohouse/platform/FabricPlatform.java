package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.actions.ScreenEvents;
import com.drakou111.neohouse.actions.actiontypes.WaitForAction;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString())
                .orElse("v?");
    }

    @Override
    public void watchForScreen(WaitForAction action) {
        ScreenEvents.watch(action);
    }
}
