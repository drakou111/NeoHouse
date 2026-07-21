package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class RenderHelper {
    private RenderHelper() {}

    public static void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.renderOutline(x, y, w, h, color);
    }

    public static void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor) {
        // Call the real API directly (compiled against this MC version so it remaps for
        // Fabric production). Routing through name-based reflection broke on Fabric because
        // production runtime uses intermediary names, so the logo never resolved.
        g.blitSprite(RenderPipelines.GUI_TEXTURED,
                ResourceLocation.fromNamespaceAndPath("neohouse", "logo"), x, y, size, size, argbColor);
    }

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public static boolean hasControlDown() {
        return Screen.hasControlDown();
    }

    public static @Nullable LivingEntity createEntity(Minecraft mc, String mobId) {
        try {
            ResourceLocation loc = ResourceLocation.tryParse(mobId);
            if (loc == null || mc.level == null) return null;
            var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
            if (typeOpt.isEmpty()) return null;
            EntityType<?> type = typeOpt.get();
            var entity = type.create(mc.level, EntitySpawnReason.LOAD);
            return entity instanceof LivingEntity le ? le : null;
        } catch (Exception e) { return null; }
    }

    public static void renderSpinningEntity(GuiGraphics gfx, Minecraft mc, LivingEntity entity, int cx, int cy, int scale, float angleDeg) {
        org.joml.Quaternionf q = com.mojang.math.Axis.YP.rotationDegrees(angleDeg);
        InventoryScreen.renderEntityInInventory(gfx, cx - scale, cy - scale * 2, cx + scale, cy, (float) scale, new org.joml.Vector3f(0, 0, 0), q, new org.joml.Quaternionf(), entity);
    }
}
