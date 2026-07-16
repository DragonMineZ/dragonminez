package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EnchantedBookDTO extends EnchantedItemDTO {
    private static final String ITEM_TYPE = "enchanted_book";
    private static final ResourceLocation ENCHANTED_BOOK_ID = ForgeRegistries.ITEMS.getKey(Items.ENCHANTED_BOOK);

    public EnchantedBookDTO(Map<ResourceLocation, Integer> enchantments) {
        super(ITEM_TYPE, ENCHANTED_BOOK_ID, 1, enchantments);
    }

    @Override
    protected void applyEnchantments(ItemStack itemStack) {
        if (this.enchantments == null || this.enchantments.isEmpty()) {
            return;
        }

        this.enchantments.forEach((id, level) ->
                EnchantedBookItem.addEnchantment(
                        itemStack,
                        getEnchantmentInstance(id, level)
                )
        );
    }

    protected EnchantmentInstance getEnchantmentInstance(ResourceLocation id, Integer level) {
        return new EnchantmentInstance(this.getEnchantment(id, level), level);
    }
}
