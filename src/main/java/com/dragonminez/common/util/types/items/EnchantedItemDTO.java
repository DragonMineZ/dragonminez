package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.HashMap;
import java.util.Map;

/**
 * Enchanted item DTO. Enchantment application in 1.21 is datapack/registry-bound;
 * runtime stacks are created unenchanted unless a registry lookup is provided later.
 * JSON still stores enchantment ids for datapack/quest definitions.
 */
public class EnchantedItemDTO extends GenericItemDTO {
    protected Map<String, Integer> enchantments = new HashMap<>();

    public EnchantedItemDTO(String itemId, int count) {
        super("enchanted_item", itemId, count);
    }

    public EnchantedItemDTO(String itemId, int count, Map<String, Integer> enchantments) {
        super("enchanted_item", itemId, count);
        this.enchantments = enchantments;
    }

    public EnchantedItemDTO(String itemType, String itemId, int count) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.count = count;
    }

    public EnchantedItemDTO(String itemType, String itemId, int count, Map<String, Integer> enchantments) {
        super(itemType, itemId, count);
        this.enchantments = enchantments;
    }

    @Override
    public ItemStack getItemStack() {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(this.getItemId()));
        if (item != null) {
            return new ItemStack(item, this.count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, EnchantedItemDTO.class);
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Integer> getEnchantments() {
        return this.enchantments;
    }

    @java.lang.SuppressWarnings("all")
    public void setEnchantments(final Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    @java.lang.SuppressWarnings("all")
    public EnchantedItemDTO() {
    }
}
