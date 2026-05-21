package com.drakou111.neohouse.platform;

import com.drakou111.neohouse.CancelKeybind;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NeoForgePlatform implements Platform {

    // Cached once per JVM session. Field (not value) is used so GUI_TEXTURED is read fresh each frame.
    private static Field   pipelineField = null;  // RenderPipelines.GUI_TEXTURED field
    private static Method  blitSprite    = null;  // blitSprite(Pipeline, Identifier, int,int,int,int,int)
    private static Object  cachedLoc     = null;  // Identifier("neohouse", "logo")
    private static boolean resolved      = false;
    // Reset on persistent failure so we retry rather than silently giving up.
    private static int     failCount     = 0;
    private static final int MAX_FAILS   = 5;

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(c -> "v" + c.getModInfo().getVersion())
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
                // Pipeline: com.mojang.blaze3d.pipeline.RenderPipeline
                Class<?> pipelineCls = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline");

                // GUI_TEXTURED lives in net.minecraft.client.renderer.RenderPipelines
                Class<?> pipelinesCls = Class.forName("net.minecraft.client.renderer.RenderPipelines");
                pipelineField = pipelinesCls.getField("GUI_TEXTURED");

                // Identifier (1.21.11) or ResourceLocation (older)
                Class<?> locCls;
                try   { locCls = Class.forName("net.minecraft.resources.Identifier"); }
                catch (ClassNotFoundException e) { locCls = Class.forName("net.minecraft.resources.ResourceLocation"); }
                cachedLoc = locCls.getMethod("fromNamespaceAndPath", String.class, String.class)
                                  .invoke(null, "neohouse", "logo");

                // Direct lookup — exact signature confirmed for 1.21.11:
                // blitSprite(RenderPipeline, Identifier, int x, int y, int w, int h, int color)
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
            // If invocation keeps failing, clear resolved so we re-attempt resolution next frame.
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
