package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.compat.ItemParseHelper;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class CreativeGiveAction extends Action {
    private final String rawItemData;

    public CreativeGiveAction(String rawItemData) {
        this.rawItemData = rawItemData;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null || mc.level == null) return true;

        if (!mc.player.isCreative()) {
            throw new InterruptActionException("Player must be in creative mode to give items.");
        }

        ItemStack finalStack = ItemStack.EMPTY;

        if (rawItemData != null && !rawItemData.isBlank()) {
            try {
                Optional<ItemStack> parsedStack = ItemParseHelper.parse(rawItemData, mc.level);
                finalStack = parsedStack.filter(stack -> !stack.isEmpty()).orElse(ItemStack.EMPTY);
            } catch (CommandSyntaxException e) {
                throw new InterruptActionException("Tried to get an item with invalid data.");
            }
        }

        int creativeSlot = mc.player.containerMenu.slots.size() - 29; // hotbar 8th slot
        mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(creativeSlot, finalStack.copy()));
        return true;
    }

    @Override
    public boolean requiresCreative() {
        return true;
    }
}