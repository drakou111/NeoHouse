package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Client state whose home moved in 26.2 (1.21.x to 26.1: on Minecraft). */
public final class ClientUi {
    private ClientUi() {}

    /** The open screen, or null in game. */
    public static Screen screen(Minecraft mc) {
        return mc.screen;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    public static PlayerTabOverlay tabList(Minecraft mc) {
        return mc.gui.getTabList();
    }

    /** True for the gray stained glass pane Housing uses as an "Empty Slot" in custom menus. */
    public static boolean isGrayGlassPane(ItemStack stack) {
        return stack.is(Items.GRAY_STAINED_GLASS_PANE);
    }
}
