package com.dragonminez.client.util;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorTextureResolver {

    private static final Map<ResourceLocation, Boolean> EXISTS_CACHE = new ConcurrentHashMap<>();

    private ArmorTextureResolver() {}

    public static void clearCache() {
        EXISTS_CACHE.clear();
    }

    public static boolean textureExists(ResourceLocation texture) {
        return EXISTS_CACHE.computeIfAbsent(texture,
                loc -> Minecraft.getInstance().getResourceManager().getResource(loc).isPresent());
    }

    public static boolean isDamaged(ItemStack stack) {
        if (stack == null) return false;
        int max = stack.getMaxDamage();
        return max > 0 && stack.getDamageValue() > max / 2;
    }

    public static ResourceLocation resolve(String itemId, EquipmentSlot slot, ItemStack stack) {
        boolean legs = slot == EquipmentSlot.LEGS;

        if (isDamaged(stack)) {
            ResourceLocation damaged = texture(itemId, legs ? "_damaged_layer2" : "_damaged_layer1");
            if (textureExists(damaged)) return damaged;
        }

        return texture(itemId, legs ? "_layer2" : "_layer1");
    }

    private static ResourceLocation texture(String itemId, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/" + itemId + suffix + ".png");
    }
}
