package com.dragonminez.common.util.types.items;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
@Setter
@NoArgsConstructor
public class GenericItemDTO {
    protected String itemType;
    protected String itemId;
    protected int count = 1;

    public GenericItemDTO(String itemId, int count) {
        this.itemType = "generic_item";
        this.itemId = itemId;
        this.count = count;
    }

    public GenericItemDTO(String itemType, String itemId, int count) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.count = count;
    }

    public ItemStack getItemStack() {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(this.getItemId()));
        if (item != null) {
            return new ItemStack(item, this.count);
        }
        return ItemStack.EMPTY;
    }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, GenericItemDTO.class);
    }
}
