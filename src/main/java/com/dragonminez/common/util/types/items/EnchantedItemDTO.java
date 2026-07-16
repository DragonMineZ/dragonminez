package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
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
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(this.getItemId()));
        if (item != null) {
            var itemStack = new ItemStack(item, this.count);
            if (this.enchantments != null && !this.enchantments.isEmpty()) {
                EnchantmentHelper.setEnchantments(this.getEnchantmentMap(), itemStack);
            }
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, EnchantedItemDTO.class);
    }

    protected Map<Enchantment, Integer> getEnchantmentMap() {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        this.enchantments.keySet().forEach(key -> {
            Enchantment value = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(key));
            enchantments.put(value, this.enchantments.get(key));
        });
        return enchantments;
    }
}
