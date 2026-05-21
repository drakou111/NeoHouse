package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.CancelKeybind;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FabricPlatform implements Platform {

    private static Field   pipelineField = null;
    private static Method  blitSprite    = null;
    private static Object  cachedLoc     = null;
    private static boolean resolved      = false;
    private static int     failCount     = 0;
    private static final int MAX_FAILS   = 5;

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString())
                .orElse("v?");
    }

    @Override
    public String getCancelKeyName() {
        return CancelKeybind.CANCEL_KEY.getTranslatedKeyMessage().getString();
    }

    @Override
    public void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor) {
        if (!resolved) {
            try {
                Class<?> pipelineCls  = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline");
                Class<?> pipelinesCls = Class.forName("net.minecraft.client.renderer.RenderPipelines");
                pipelineField = pipelinesCls.getField("GUI_TEXTURED");

                Class<?> locCls;
                try   { locCls = Class.forName("net.minecraft.resources.Identifier"); }
                catch (ClassNotFoundException e) { locCls = Class.forName("net.minecraft.resources.ResourceLocation"); }
                cachedLoc = locCls.getMethod("fromNamespaceAndPath", String.class, String.class)
                                  .invoke(null, "neohouse", "logo");

                blitSprite = g.getClass().getMethod("blitSprite",
                        pipelineCls, locCls, int.class, int.class, int.class, int.class, int.class);

                if (pipelineField != null && blitSprite != null && cachedLoc != null) {
                    resolved  = true;
                    failCount = 0;
                }
            } catch (Exception ignored) {}
        }

        if (!resolved || blitSprite == null || pipelineField == null || cachedLoc == null) return;

        try {
            Object pipeline = pipelineField.get(null);
            if (pipeline == null) return;
            blitSprite.invoke(g, pipeline, cachedLoc, x, y, size, size, argbColor);
            failCount = 0;
        } catch (Exception e) {
            if (++failCount >= MAX_FAILS) {
                resolved      = false;
                failCount     = 0;
                blitSprite    = null;
                pipelineField = null;
                cachedLoc     = null;
            }
        }
    }
}
