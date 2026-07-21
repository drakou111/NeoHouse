package com.drakou111.neohouse.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RenderHelper {
    private RenderHelper() {}

    public static void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.renderOutline(x, y, w, h, color);
    }

    public static void renderLogo(GuiGraphics g, int x, int y, int size, int argbColor) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("neohouse", "logo"), x, y, size, size, argbColor);
    }

    public static boolean hasShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean hasControlDown() {
        return Minecraft.getInstance().hasControlDown();
    }

    public static @Nullable LivingEntity createEntity(Minecraft mc, String mobId) {
        try {
            Identifier loc = Identifier.tryParse(mobId);
            if (loc == null || mc.level == null) return null;
            var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
            if (typeOpt.isEmpty()) return null;
            EntityType<?> type = typeOpt.get();
            var entity = type.create(mc.level, EntitySpawnReason.LOAD);
            return entity instanceof LivingEntity le ? le : null;
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderSpinningEntity(GuiGraphics gfx, Minecraft mc, LivingEntity entity, int cx, int cy, int scale, float angleDeg) {
        try {
            EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
            EntityRenderState state = (EntityRenderState) renderer.createRenderState(entity, 1.0f);
            state.lightCoords = 15728880;
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof LivingEntityRenderState ls) {
                ls.bodyRot = 180f;
                ls.yRot = 0f;
                ls.xRot = 0f;
            }
            Quaternionf spinQ = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(angleDeg));
            float offsetY = state.boundingBoxHeight / 2.0f;
            int vp = scale * 2;
            gfx.submitEntityRenderState(state, (float) scale, new Vector3f(0, offsetY, 0),
                spinQ, new Quaternionf(), cx - vp, cy - vp * 2, cx + vp, cy);
        } catch (Exception e) {
            float r = 400f;
            float rad = (float) Math.toRadians(angleDeg);
            float mx = cx + (float) Math.sin(rad) * r;
            float my = cy + (float) Math.cos(rad) * r;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                gfx, cx - scale, cy - scale * 2, cx + scale, cy, scale, 0f, mx, my, entity);
        }
    }
}
