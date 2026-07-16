package com.dragonminez.common.util.types.items;

import com.google.gson.JsonSyntaxException;
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
    private static final String ITEM_TYPE = "enchanted_item";

    protected Map<ResourceLocation, Integer> enchantments = new HashMap<>();

    public EnchantedItemDTO(ResourceLocation itemId, int count) {
        super(ITEM_TYPE, itemId, count);
    }

    public EnchantedItemDTO(ResourceLocation itemId, int count, Map<ResourceLocation, Integer> enchantments) {
        super(ITEM_TYPE, itemId, count);
        this.enchantments = enchantments;
    }

    public EnchantedItemDTO(String itemType, ResourceLocation itemId, int count) {
        super(itemType, itemId, count);
    }

    public EnchantedItemDTO(String itemType, ResourceLocation itemId, int count, Map<ResourceLocation, Integer> enchantments) {
        super(itemType, itemId, count);
        this.enchantments = enchantments;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack itemStack = super.getItemStack();
        this.applyEnchantments(itemStack);
        return itemStack;
    }

    protected void applyEnchantments(ItemStack itemStack) {
        if (this.enchantments == null || this.enchantments.isEmpty()) {
            return;
        }

        EnchantmentHelper.setEnchantments(this.getEnchantmentMap(), itemStack);
    }

    protected Map<Enchantment, Integer> getEnchantmentMap() {
        Map<Enchantment, Integer> enchantmentMap = new HashMap<>(this.enchantments.size());
        this.enchantments.forEach((id, level) ->
                enchantmentMap.put(
                        this.getEnchantment(id, level),
                        level
                )
        );
        return enchantmentMap;
    }

    protected Enchantment getEnchantment(ResourceLocation id, Integer level) {
        if (id == null) {
            throw new JsonSyntaxException(getErrorPrefix() + " enchantment id must not be null.");
        }

        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment == null) {
            throw new JsonSyntaxException(getErrorPrefix() + " unknown enchantment '" + id + "'.");
        }

        if (level == null || level < 1) {
            throw new JsonSyntaxException(getErrorPrefix() + " enchantment '" + id + "' has invalid level " + level + ".");
        }

        return enchantment;
    }
}
