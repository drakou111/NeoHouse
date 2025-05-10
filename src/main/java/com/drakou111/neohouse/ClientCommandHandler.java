package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.factories.MovementFactory;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = NeoHouse.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientCommandHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("neohouse").then(
                Commands.literal("example").executes(ctx -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        List<Action> actions = new ArrayList<>();

                        actions.add(new ActionBuilder().sendChatAndWait(com.drakou111.neohouse.constants.Commands.FUNCTIONS).get());
                        actions.add(MovementFactory.recreateFunction("testing", "some testing", null, true, 45));

                        SequenceManager.startAction(new GroupAction(actions));
                    }
                    return 1; // success
                })
            )
        );
    }
}
