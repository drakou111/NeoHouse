package com.drakou111.neohouse.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Version-specific sound-registry access. Isolated here because the id type is
 * named {@code Identifier} in 1.21.9+ but {@code ResourceLocation} in 1.21.8 and
 * earlier — {@code common/src/main} must stay type-agnostic (Strings only).
 */
public final class SoundCompat {

    private SoundCompat() {}

    /** Paths of all {@code minecraft:}-namespace sound events currently registered. */
    public static Set<String> soundEventPaths() {
        Set<String> out = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            if ("minecraft".equals(id.getNamespace())) out.add(id.getPath());
        }
        return out;
    }

    /** A playable {@link SoundEvent} for a {@code minecraft:}-namespace sound path. */
    public static SoundEvent soundEvent(String path) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("minecraft", path));
    }
}
