package com.drakou111.neohouse;

import com.drakou111.neohouse.platform.FabricPlatform;
import com.drakou111.neohouse.platform.Platforms;
import net.fabricmc.api.ClientModInitializer;

public class NeoHouseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platforms.init(new FabricPlatform());
        ClientCommandHandler.register();
        TickDriver.register();
    }
}
