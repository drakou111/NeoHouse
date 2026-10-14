package com.drakou111.neohouse.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Optional;

public final class RegistryHelper {
    private RegistryHelper() {}

    public static Optional<Item> lookupItem(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id));
    }
}
