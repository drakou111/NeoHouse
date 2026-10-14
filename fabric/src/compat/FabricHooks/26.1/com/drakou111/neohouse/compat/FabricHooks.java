package com.drakou111.neohouse.compat;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** Fabric API registrations whose classes were renamed in 26.1 (26.x API). */
public final class FabricHooks {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("neohouse", "script_overlay");

    private FabricHooks() {}

    @SuppressWarnings("unchecked")
    public static <K extends KeyMapping> K registerKey(K key) {
        return (K) KeyMappingHelper.registerKeyMapping(key);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommands.literal(name);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return ClientCommands.argument(name, type);
    }

    /** Draws on the HUD every frame, after the vanilla HUD. */
    public static void registerHud(Consumer<Gfx> drawer) {
        HudElementRegistry.addLast(HUD_ID, (graphics, delta) -> drawer.accept(new Gfx(graphics)));
    }
}
