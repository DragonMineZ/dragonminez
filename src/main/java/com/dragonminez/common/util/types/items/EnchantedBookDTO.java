package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EnchantedBookDTO extends EnchantedItemDTO {
    protected String itemType;
    protected Map<String, Integer> enchantments = new HashMap<>();

    public EnchantedBookDTO(Map<String, Integer> enchantments) {
        super("enchanted_book", "minecraft:enchanted_book", 1, enchantments);
    }

    @Override
    public ItemStack getItemStack() {
        var itemStack = new ItemStack(Items.ENCHANTED_BOOK);
        enchantments.keySet().forEach(key -> {
            EnchantedBookItem.addEnchantment(
                    itemStack,
                    getEnchantmentInstance(key, enchantments.get(key))
            );
        });
        return itemStack;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, EnchantedBookDTO.class);
    }

    protected EnchantmentInstance getEnchantmentInstance(String id, Integer level) {
        var resourceLocation = ResourceLocation.tryParse(id);
        if (resourceLocation != null) {
            var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(resourceLocation);
            if (enchantment != null) {
                return new EnchantmentInstance(enchantment, level);
            }
            return null;
        }
        return null;

    }
}
