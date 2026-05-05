package com.drakou111.neohouse.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyMappingCompat extends KeyMapping {
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("neohouse", "neohouse"));

    public KeyMappingCompat(String name, int key) {
        super(name, key, CATEGORY);
    }
}