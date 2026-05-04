package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class InventoryUniquenessHint implements HintProvider {
    private final String text;
    private final int min;
    private final int max;

    public InventoryUniquenessHint(String text, int min, int max) {
        this.text = text;
        this.min  = min;
        this.max  = max;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        Set<String> unique = new HashSet<>();
        for (ItemStack stack : mc.player.getInventory().items) {
            if (!stack.isEmpty()) unique.add(stack.getItem().toString());
        }
        return unique.size() >= min && unique.size() <= max;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
