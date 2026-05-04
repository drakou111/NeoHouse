package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class HasItemHint implements HintProvider {
    private final String text;
    private final Set<String> items;
    private final int minCount;

    public HasItemHint(String text, Set<String> items, int minCount) {
        this.text     = text;
        this.items    = items;
        this.minCount = minCount;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        int count = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (items.contains(id.getPath())) count += stack.getCount();
        }
        return count >= minCount;
    }

    @Override
    public String getText(Minecraft mc) {
        return text;
    }
}
