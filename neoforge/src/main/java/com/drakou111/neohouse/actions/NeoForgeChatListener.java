package com.drakou111.neohouse.actions;

import com.drakou111.neohouse.NHConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeChatListener {

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        String plain = event.getMessage().getString();
        ChatEvents.onChatMessage(plain);
    }
}
