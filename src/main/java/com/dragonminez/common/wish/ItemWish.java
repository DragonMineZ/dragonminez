package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemWish extends Wish {
    private final String itemId;
    private final int count;

    public ItemWish(String name, String description, String itemId, int count) {
        super(name, description);
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public void grant(ServerPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item != null) {
            player.getInventory().add(new ItemStack(item, count));
        } else {
            LogUtil.warn(Env.COMMON, "Item with id " + itemId + " not found.");
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "item");
        json.addProperty("name", getName());
        json.addProperty("description", getDescription());
        json.addProperty("item_id", itemId);
        json.addProperty("count", count);
        return json;
    }
}
