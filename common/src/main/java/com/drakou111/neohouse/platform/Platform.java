package com.drakou111.neohouse.platform;

import net.minecraft.client.gui.GuiGraphics;

public interface Platform {
    boolean isModLoaded(String modId);
    String getModVersion(String modId);
    String getCancelKeyName();
    void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor);
}
