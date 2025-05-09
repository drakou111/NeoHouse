package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InteruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.ClickType;

import java.lang.reflect.Field;

/**
 * Types `name` into the anvil textbox, then clicks the output slot.
 */
public class AnvilAction extends Action {
    private final String name;
    private Field nameField;

    public AnvilAction(String name) {
        this.name = name;
        try {
            nameField = AnvilScreen.class.getDeclaredField("name");
            nameField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!(mc.screen instanceof AnvilScreen anvil)) throw new InteruptActionException("Not in anvil screen.");

        EditBox edit;
        try {
            edit = (EditBox) nameField.get(anvil);
        } catch (IllegalAccessException e) {
            throw new InteruptActionException(e.getMessage());
        }

        if (edit == null || edit.getValue() == null || edit.getValue().isEmpty()) return false;

        edit.setValue(name);
        anvil.getMenu().setItemName(name);
        mc.gameMode.handleInventoryMouseClick(anvil.getMenu().containerId, 2, 0, ClickType.PICKUP, mc.player);
        return true;
    }
}
