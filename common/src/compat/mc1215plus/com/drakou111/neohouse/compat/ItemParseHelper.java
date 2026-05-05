package com.drakou111.neohouse.compat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ItemParseHelper {
    private ItemParseHelper() {}

    public static Optional<ItemStack> parse(String snbt, ClientLevel level) throws CommandSyntaxException {
        CompoundTag nbt = TagParser.parseCompoundFully(snbt);
        RegistryOps<net.minecraft.nbt.Tag> ops =
                level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, nbt).result();
    }
}
