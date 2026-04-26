package com.drakou111.neohouse;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(NeoHouse.MOD_ID)
public class NeoHouse
{
    public static final String MOD_ID = "neohouse";

    public NeoHouse(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(CancelKeybind::registerKeys);
    }
}
