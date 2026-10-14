package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
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
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((argbColor >> 16) & 0xFF) / 255f, ((argbColor >> 8) & 0xFF) / 255f, (argbColor & 0xFF) / 255f, ((argbColor >> 24) & 0xFF) / 255f);
        g.blit(RenderType::guiTextured, ResourceLocation.fromNamespaceAndPath("neohouse", "textures/logo.png"), x, y, 0.0f, 0.0f, size, size, 512, 512);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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
        float r = 400f;
        float rad = (float) Math.toRadians(angleDeg);
        float mx = cx + (float) Math.sin(rad) * r;
        float my = cy + (float) Math.cos(rad) * r;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            gfx, cx - scale, cy - scale * 2, cx + scale, cy, scale, 0f, mx, my, entity);
    }
}
