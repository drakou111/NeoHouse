package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.utils.ActionBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class DeleteAllCommandsAction extends Action {
    private Action current = null;

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (current == null) {
            if (menu.slots.size() <= 10 || !menu.slots.get(10).hasItem()) return true;
            String fullName = menu.slots.get(10).getItem().getHoverName().getString();
            // Command items in the list are shown as "/commandName"; strip the slash for removeCommandIfPresent
            String name = fullName.startsWith("/") ? fullName.substring(1) : fullName;
            current = new ActionBuilder().removeCommandIfPresent(name).get();
        }

        if (current.run(mc)) current = null;
        return false;
    }
}
