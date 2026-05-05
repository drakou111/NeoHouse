package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class TickDriver {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SequenceManager.tick(Minecraft.getInstance());
    }
}
