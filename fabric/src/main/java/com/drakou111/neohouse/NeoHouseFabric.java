package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.ChatEvents;
import com.drakou111.neohouse.platform.FabricPlatform;
import com.drakou111.neohouse.platform.Platforms;
import com.drakou111.neohouse.utils.ScriptOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public class NeoHouseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platforms.init(new FabricPlatform());
        ClientCommandHandler.register();
        TickDriver.register();
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            ChatEvents.onChatMessage(message.getString());
            return true;
        });
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            ScriptOverlayRenderer.render(guiGraphics, mc, w, h);
        });
    }
}
