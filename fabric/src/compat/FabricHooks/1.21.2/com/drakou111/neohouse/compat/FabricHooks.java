package com.drakou111.neohouse.compat;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;

import java.util.function.Consumer;

/** Fabric API registrations whose classes were renamed in 26.1 (1.21.x API). */
public final class FabricHooks {
    private FabricHooks() {}

    @SuppressWarnings("unchecked")
    public static <K extends KeyMapping> K registerKey(K key) {
        return (K) KeyBindingHelper.registerKeyBinding(key);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommandManager.literal(name);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return ClientCommandManager.argument(name, type);
    }

    /** Draws on the HUD every frame, after the vanilla HUD. */
    public static void registerHud(Consumer<Gfx> drawer) {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> drawer.accept(new Gfx(graphics)));
    }
}
