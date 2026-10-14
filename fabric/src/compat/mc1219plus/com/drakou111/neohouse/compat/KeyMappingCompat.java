package com.drakou111.neohouse.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class KeyMappingCompat extends KeyMapping {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("neohouse", "neohouse"));

    public KeyMappingCompat(String name, int key) {
        super(name, key, CATEGORY);
    }

    public int getKeyCode() {
        return this.key.getValue();
    }
}
