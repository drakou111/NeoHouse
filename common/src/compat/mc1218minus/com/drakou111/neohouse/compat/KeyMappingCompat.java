package com.drakou111.neohouse.compat;

import net.minecraft.client.KeyMapping;

public class KeyMappingCompat extends KeyMapping {
    private static final String CATEGORY = "key.categories.neohouse";
    private final int keyCode;

    public KeyMappingCompat(String name, int key) {
        super(name, key, CATEGORY);
        this.keyCode = key;
    }

    public int getKeyCode() {
        return this.keyCode;
    }
}
