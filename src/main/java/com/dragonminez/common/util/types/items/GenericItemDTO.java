package com.dragonminez.common.util.types.items;

 import com.dragonminez.common.util.gson.GsonUtils;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
@Setter
@NoArgsConstructor
public class GenericItemDTO {
    private final String ITEM_TYPE = "generic_item";

    protected String itemType;
    protected ResourceLocation itemId;
    protected int count = 1;

    public GenericItemDTO(ResourceLocation itemId, int count) {
        this.itemType = ITEM_TYPE;
        this.itemId = itemId;
        this.count = count;
    }

    public GenericItemDTO(Item item, int count) {
        this.itemType = ITEM_TYPE;
        this.itemId = ForgeRegistries.ITEMS.getKey(item);
        this.count = count;
    }

    public GenericItemDTO(String itemType, ResourceLocation itemId, int count) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.count = count;
    }

    public ItemStack getItemStack() {
        if (this.itemId == null) {
            throw new JsonSyntaxException(this.getClass().getSimpleName() + ": itemId must not be null.");
        }

        if (this.count < 1) {
            throw new JsonSyntaxException(this.getErrorPrefix() + " count must be >= 1.");
        }

        var item = ForgeRegistries.ITEMS.getValue(this.getItemId());
        if (item == null) {
            throw new JsonSyntaxException(this.getErrorPrefix() + " is unknown.");
        }
        return new ItemStack(item, this.count);
    }

    public String toJson() {
        return GsonUtils.GSON.toJson(this);
    }

    protected final String getErrorPrefix() {
        return this.getClass().getSimpleName() + ": '" + this.itemId + "'";
    }
}
