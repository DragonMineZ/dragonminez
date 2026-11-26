package com.dragonminez.compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Adapts Forge 1.20 ItemStack packet helpers to the registry-aware Minecraft 1.21 stream codec. */
public final class LegacyItemStackNetworkCodec {
    private LegacyItemStackNetworkCodec() {}

    public static ItemStack read(FriendlyByteBuf buffer) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(requireRegistryBuffer(buffer));
    }

    public static void write(FriendlyByteBuf buffer, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(requireRegistryBuffer(buffer), stack == null ? ItemStack.EMPTY : stack);
    }

    private static RegistryFriendlyByteBuf requireRegistryBuffer(FriendlyByteBuf buffer) {
        if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
            throw new IllegalArgumentException("ItemStack payload requires a NeoForge RegistryFriendlyByteBuf");
        }
        return registryBuffer;
    }
}
