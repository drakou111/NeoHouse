package com.drakou111.neohouse.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class RenderHelper {
    private RenderHelper() {}

    public static void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.renderOutline(x, y, w, h, color);
    }

    public static void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((argbColor >> 16) & 0xFF) / 255f, ((argbColor >> 8) & 0xFF) / 255f, (argbColor & 0xFF) / 255f, ((argbColor >> 24) & 0xFF) / 255f);
        g.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("neohouse", "textures/logo.png"), x, y, 0.0f, 0.0f, size, size, 512, 512);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
