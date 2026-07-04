package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.CancelKeybind;
import com.drakou111.neohouse.TickDriver;
import net.minecraft.client.gui.screens.Screen;
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
    public String getCancelKeyName() {
        return CancelKeybind.CANCEL_KEY.getTranslatedKeyMessage().getString();
    }

    @Override
    public void openScreenNextTick(Screen screen) {
        TickDriver.openScreenNextTick(screen);
    }
}
