package com.drakou111.neohouse.actions.actiontypes;

import com.drakou111.neohouse.actions.Action;
import com.drakou111.neohouse.actions.ActionResult;
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

    public AnvilAction(String name) {
        this.name = name;
    }

    @Override
    public ActionResult run(Minecraft mc) {
        if (mc.screen instanceof AnvilScreen anvil && mc.player != null && mc.gameMode != null) {
            try {
                Field nameField = AnvilScreen.class.getDeclaredField("name");
                nameField.setAccessible(true);
                EditBox edit = (EditBox) nameField.get(anvil);
                edit.setValue(name);
                anvil.getMenu().setItemName(name);
                mc.gameMode.handleInventoryMouseClick(anvil.getMenu().containerId, 2, 0, ClickType.PICKUP, mc.player);

                return new ActionResult(true);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                return new ActionResult(true, "Reflection failure: " + e.getMessage(), false);
            }
        }

        return new ActionResult(true, "Expected to be in an anvil menu.", false);
    }
}
