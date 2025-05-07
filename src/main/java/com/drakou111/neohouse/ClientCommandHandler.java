package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionFactory;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

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
                        Action cond = ActionFactory.group_addActionConditional(true, null, null, null);
                        Action start = new GroupAction(List.of(ActionFactory.goto_function("vvv", false), cond));
                        SequenceManager.startAction(start);
                    }
                    return 1; // success
                })
            )
        );
    }
}
