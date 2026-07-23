package com.dragonminez.common.util.types.items;

import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TrimmedArmorDTO extends EnchantedItemDTO {
    private static final String ITEM_TYPE = "trimmed_armor";

    protected ResourceLocation material;
    protected ResourceLocation pattern;

    public TrimmedArmorDTO(ResourceLocation itemId, ResourceLocation material, ResourceLocation pattern) {
        super(ITEM_TYPE, itemId, 1);
        this.material = material;
        this.pattern = pattern;
    }

    public TrimmedArmorDTO(ResourceLocation itemId, Map<ResourceLocation, Integer> enchantments, ResourceLocation material, ResourceLocation pattern) {
        super(ITEM_TYPE, itemId, 1, enchantments);
        this.material = material;
        this.pattern = pattern;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack itemStack = super.getItemStack();
        if (itemStack.isEmpty()) {
            return itemStack;
        }
        this.applyTrim(itemStack);
        return itemStack;
    }

    protected void applyTrim(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ArmorItem)) {
            throw new JsonSyntaxException(this.getErrorPrefix() + "' item does not extend ArmorItem.");
        }

        if (material == null) {
            throw new JsonSyntaxException(this.getErrorPrefix() + "' material must not be null.");
        }

        if (pattern == null) {
            throw new JsonSyntaxException(this.getErrorPrefix() + "' pattern must not be null.");
        }

        CompoundTag trimTag = itemStack.getOrCreateTagElement("Trim");
        trimTag.putString("material", material.toString());
        trimTag.putString("pattern", pattern.toString());
    }
}
