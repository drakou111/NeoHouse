package com.drakou111.neohouse;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.factories.ActionFactory;
import com.drakou111.neohouse.factories.ConditionFactory;
import com.drakou111.neohouse.factories.MovementFactory;
import com.drakou111.neohouse.actions.actiontypes.GroupAction;
import com.drakou111.neohouse.constants.RestrictedOperator;
import com.drakou111.neohouse.constants.RestrictionAction;
import com.drakou111.neohouse.constants.RestrictionCondition;
import com.drakou111.neohouse.factories.OtherFactory;
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
                        actions.add(MovementFactory.goToFunction(false, "vvv"));
                        actions.add(OtherFactory.deleteAll());
                        actions.add(ActionFactory.addActionMaxHealth(RestrictionAction.NONE, "50", RestrictedOperator.INCREMENT, true, null));

                        Action conditions = new GroupAction(List.of(
                                ConditionFactory.addConditionSneaking(RestrictionCondition.NONE, false, "Idk, just like check if shifting")
                        ));
                        Action ifs = new GroupAction(List.of(
                                ActionFactory.addActionTitle(RestrictionAction.CONDITIONAL, "&6&lHello everybody", "&7How are you?", 3, 5, 3, "Hey")
                        ));
                        Action elses = null;

                        actions.add(ActionFactory.addActionConditional(RestrictionAction.NONE, false, conditions, ifs, elses, "Some testing"));
                        actions.add(MovementFactory.exit(true));
                        SequenceManager.startAction(new GroupAction(actions));
                    }
                    return 1; // success
                })
            )
        );
    }
}
