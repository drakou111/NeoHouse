package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Client state whose home moved in 26.2 (26.2+: on Gui). */
public final class ClientUi {
    private ClientUi() {}

    /** The open screen, or null in game. */
    public static Screen screen(Minecraft mc) {
        return mc.gui.screen();
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    public static PlayerTabOverlay tabList(Minecraft mc) {
        return mc.gui.hud.getTabList();
    }

    /** True for the gray stained glass pane Housing uses as an "Empty Slot" in custom menus. */
    public static boolean isGrayGlassPane(ItemStack stack) {
        return stack.is(Items.STAINED_GLASS_PANE.gray());
    }
}
