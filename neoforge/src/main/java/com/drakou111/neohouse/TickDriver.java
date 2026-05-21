package com.drakou111.neohouse;

import com.drakou111.neohouse.utils.MessageUtils;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = NHConstants.MOD_ID, value = Dist.CLIENT)
public class TickDriver {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SequenceManager.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (!KeyCompatHelper.matches(CancelKeybind.CANCEL_KEY, event)) return;
        if (!SequenceManager.isRunning()) return;
        SequenceManager.cancelAction();
        MessageUtils.debugToChat("Script cancelled.");
    }
}
