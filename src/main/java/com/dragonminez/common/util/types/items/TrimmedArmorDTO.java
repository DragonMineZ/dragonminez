package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TrimmedArmorDTO extends EnchantedItemDTO {
    private String material;
    private String pattern;

    public TrimmedArmorDTO(String itemId, String material, String pattern) {
        super("trimmed_armor", itemId, 1);
        this.material = material;
        this.pattern = pattern;
    }

    public TrimmedArmorDTO(String itemId, Map<String, Integer> enchantments, String material, String pattern) {
        super("trimmed_armor", itemId, 1, enchantments);
        this.material = material;
        this.pattern = pattern;
    }

    @Override
    public ItemStack getItemStack() {
        var itemStack = super.getItemStack();
        CompoundTag trimTag = new CompoundTag();
        trimTag.putString("material", material);
        trimTag.putString("pattern", pattern);

        itemStack.getOrCreateTag().put("Trim", trimTag);
        return itemStack;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, TrimmedArmorDTO.class);
    }
}
