package com.drakou111.neohouse.compat;

import net.minecraft.client.KeyMapping;

public class KeyMappingCompat extends KeyMapping {
    private static final String CATEGORY = "key.categories.neohouse";

    public KeyMappingCompat(String name, int key) {
        super(name, key, CATEGORY);
    }
}
