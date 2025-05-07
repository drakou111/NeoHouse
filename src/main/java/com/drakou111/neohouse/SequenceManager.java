package com.drakou111.neohouse;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.drakou111.neohouse.actions.ActionSequence;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SequenceManager {
    /** The currently running sequence; null if none. */
    private static ActionSequence currentSequence = null;

    /** Call this to start a new sequence. */
    public static void startSequence(ActionSequence seq) {
        currentSequence = seq;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (currentSequence != null) {
            boolean stillRunning = currentSequence.tick(mc);
            if (!stillRunning) {
                MessageUtils.debugToChat("Done!");
                currentSequence = null;
            }
        }
    }
}
