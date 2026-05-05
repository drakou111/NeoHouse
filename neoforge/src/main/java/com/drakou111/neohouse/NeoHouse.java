package com.drakou111.neohouse;

import com.drakou111.neohouse.platform.NeoForgePlatform;
import com.drakou111.neohouse.platform.Platforms;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(NHConstants.MOD_ID)
public class NeoHouse
{
    public NeoHouse(IEventBus modEventBus, ModContainer modContainer)
    {
        Platforms.init(new NeoForgePlatform());
    }
}
