package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.exceptions.InterruptActionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.ClickType;

import java.lang.reflect.Field;

public class AnvilAction extends Action {
    private final String name;
    private Field nameField;

    public AnvilAction(String name) {
        this.name = name;
        try {
            nameField = AnvilScreen.class.getDeclaredField("name");
            nameField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("AnvilScreen.name field not found — mapping mismatch?", e);
        }
    }

    @Override
    public boolean run(Minecraft mc) {
        if (!(mc.screen instanceof AnvilScreen anvil)) throw new InterruptActionException("Not in anvil screen.");

        EditBox edit;
        try {
            edit = (EditBox) nameField.get(anvil);
        } catch (IllegalAccessException e) {
            throw new InterruptActionException("Cannot access anvil name field", e);
        }

        if (edit == null || edit.getValue() == null || edit.getValue().isEmpty()) return false;

        edit.setValue(name);
        anvil.getMenu().setItemName(name);
        mc.gameMode.handleInventoryMouseClick(anvil.getMenu().containerId, 2, 0, ClickType.PICKUP, mc.player);
        return true;
    }
}
