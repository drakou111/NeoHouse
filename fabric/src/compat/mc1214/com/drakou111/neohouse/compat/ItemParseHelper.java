package com.drakou111.neohouse.compat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ItemParseHelper {
    private ItemParseHelper() {}

    // TagParser.parseTag(String) was removed in 1.21.5; resolve once at load time.
    private static final Method PARSE_TAG;
    static {
        Method m = null;
        try { m = TagParser.class.getMethod("parseTag", String.class); }
        catch (NoSuchMethodException ignored) {}
        PARSE_TAG = m;
    }

    public static Optional<ItemStack> parse(String snbt, ClientLevel level) throws CommandSyntaxException {
        if (PARSE_TAG == null) return Optional.empty();
        try {
            CompoundTag nbt = (CompoundTag) PARSE_TAG.invoke(null, snbt);
            return ItemStack.parse(level.registryAccess(), nbt);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CommandSyntaxException) throw (CommandSyntaxException) cause;
            throw new RuntimeException("NBT parse failed", cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("NBT parse failed", e);
        }
    }
}
