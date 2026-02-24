package com.dragonminez.common.wish.wishtype;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemWish extends Wish {
    private final String itemId;
    private final int count;

    public ItemWish(String name, String description, String itemId, int count) {
        super(name, description, "item");
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public void grant(ServerPlayer player) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
        if (item != null) {
            player.getInventory().add(new ItemStack(item, count));
        } else {
            LogUtil.warn(Env.COMMON, "Item with id " + itemId + " not found.");
        }
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, ItemWish.class);
    }
}
