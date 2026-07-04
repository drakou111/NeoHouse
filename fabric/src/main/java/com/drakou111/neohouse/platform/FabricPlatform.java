package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.CancelKeybind;
import com.drakou111.neohouse.TickDriver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

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
    public String getCancelKeyName() {
        return CancelKeybind.CANCEL_KEY.getTranslatedKeyMessage().getString();
    }

    @Override
    public void openScreenNextTick(Screen screen) {
        TickDriver.openScreenNextTick(screen);
    }
}
