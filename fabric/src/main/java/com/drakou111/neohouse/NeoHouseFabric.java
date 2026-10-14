package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.ChatEvents;
import com.drakou111.neohouse.compat.FabricHooks;
import com.drakou111.neohouse.platform.FabricPlatform;
import com.drakou111.neohouse.platform.Platforms;
import com.drakou111.neohouse.utils.ScriptOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public class NeoHouseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platforms.init(new FabricPlatform());
        CancelKeybind.register();
        ClientCommandHandler.register();
        TickDriver.register();
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            ChatEvents.onChatMessage(message.getString());
            return true;
        });
        FabricHooks.registerHud(g -> {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            ScriptOverlayRenderer.render(g, mc, w, h);
        });
    }
}
