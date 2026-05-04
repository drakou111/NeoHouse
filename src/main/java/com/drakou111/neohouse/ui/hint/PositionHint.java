package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class PositionHint implements HintProvider {
    private final String text;
    private final double minX, maxX, minY, maxY, minZ, maxZ;

    public PositionHint(String text, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this.text = text;
        this.minX = minX; this.maxX = maxX;
        this.minY = minY; this.maxY = maxY;
        this.minZ = minZ; this.maxZ = maxZ;
    }

    public PositionHint(String text, double min, double max, Direction.Axis axis) {
        this(text,
             axis == Direction.Axis.X ? min : -Double.MAX_VALUE,
             axis == Direction.Axis.X ? max :  Double.MAX_VALUE,
             axis == Direction.Axis.Y ? min : -Double.MAX_VALUE,
             axis == Direction.Axis.Y ? max :  Double.MAX_VALUE,
             axis == Direction.Axis.Z ? min : -Double.MAX_VALUE,
             axis == Direction.Axis.Z ? max :  Double.MAX_VALUE);
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        Vec3 pos = mc.player.position();
        return pos.x() >= minX && pos.x() <= maxX
            && pos.y() >= minY && pos.y() <= maxY
            && pos.z() >= minZ && pos.z() <= maxZ;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
