package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.constants.HousingList;
import com.drakou111.neohouse.utils.ActionBuilder;
import com.drakou111.neohouse.utils.SequenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * With a Housing list open (functions, commands or custom menus), removes its first entry until
 * the list is empty.
 */
public class DeleteListEntriesAction extends Action {
    private static final int FIRST_ENTRY_SLOT = 10;

    private final HousingList list;
    private Action current = null;

    public DeleteListEntriesAction(HousingList list) {
        if (list != HousingList.FUNCTIONS && list != HousingList.COMMANDS && list != HousingList.MENUS)
            throw new IllegalArgumentException("Only functions, commands and menus can be deleted this way, not " + list);
        this.list = list;
    }

    @Override
    public String getDebugName() {
        return "Delete every " + this.list.singularName;
    }

    @Override
    public boolean run(Minecraft mc) {
        if (mc.player == null) return true;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return true;

        if (this.current == null) {
            if (menu.slots.size() <= FIRST_ENTRY_SLOT || !menu.slots.get(FIRST_ENTRY_SLOT).hasItem()) return true;
            String name = this.list.nameOf(menu.slots.get(FIRST_ENTRY_SLOT).getItem().getHoverName().getString());
            this.current = removal(name);
            SequenceManager.addSteps(this.current.countSteps());
        }
        if (this.current.run(mc)) this.current = null;
        return false;
    }

    private Action removal(String name) {
        ActionBuilder builder = new ActionBuilder();
        return switch (this.list) {
            case FUNCTIONS -> builder.removeFunctionIfPresent(name).get();
            case COMMANDS -> builder.removeCommandIfPresent(name).get();
            case MENUS -> builder.removeMenuIfPresent(name).get();
            default -> throw new IllegalStateException("Unexpected list " + this.list);
        };
    }
}
