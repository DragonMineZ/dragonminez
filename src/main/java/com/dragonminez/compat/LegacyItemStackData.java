package com.dragonminez.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Adapts Forge 1.20 ItemStack tag access to Minecraft 1.21.1 custom data components. */
public final class LegacyItemStackData {
    private static final net.minecraft.core.HolderLookup.Provider BUILTIN_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private LegacyItemStackData() {}

    /** Registry lookup for item-stack payloads stored outside a live level (for example inside a portable bag). */
    public static net.minecraft.core.HolderLookup.Provider registryLookup() { return BUILTIN_LOOKUP; }

    /** Returns the live custom-data tag, or null when the stack has no custom data. */
    public static CompoundTag getTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.getUnsafe();
    }

    /** Returns the live custom-data tag, creating the component if needed. */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            // CustomData.set removes empty compounds; legacy callers need a live tag to populate.
            data = CustomData.of(new CompoundTag());
            stack.set(DataComponents.CUSTOM_DATA, data);
        }
        return data.getUnsafe();
    }

    public static boolean hasTag(ItemStack stack) {
        return getTag(stack) != null;
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (stack == null || stack.isEmpty()) return;
        if (tag == null) stack.remove(DataComponents.CUSTOM_DATA);
        else CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    /** Decodes a stack using the 1.21 registry-aware item codec and the bootstrapped item registries. */
    public static ItemStack parse(CompoundTag tag) {
        return tag == null ? ItemStack.EMPTY : ItemStack.parseOptional(BUILTIN_LOOKUP, tag);
    }

    /** Encodes a stack using the registry-aware 1.21 item codec. */
    public static CompoundTag save(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        net.minecraft.nbt.Tag encoded = stack.save(BUILTIN_LOOKUP);
        return encoded instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /** Encodes a stack into an existing compound, matching the legacy save(CompoundTag) shape. */
    public static CompoundTag save(ItemStack stack, CompoundTag output) {
        if (output == null) output = new CompoundTag();
        if (stack == null || stack.isEmpty()) return output;
        net.minecraft.nbt.Tag encoded = stack.save(BUILTIN_LOOKUP, output);
        return encoded instanceof CompoundTag compound ? compound : output;
    }
}
