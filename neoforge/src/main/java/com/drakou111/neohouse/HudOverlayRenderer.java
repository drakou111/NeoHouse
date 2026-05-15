package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.ScriptOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class HudOverlayRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        ScriptOverlayRenderer.render(event.getGuiGraphics(), mc, w, h);
    }
}
