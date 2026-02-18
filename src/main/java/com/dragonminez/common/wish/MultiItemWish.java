package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MultiItemWish extends Wish {
    private final List<Tuple<String, Integer>> items;

    public MultiItemWish(String name, String description, List<Tuple<String, Integer>> items) {
        super(name, description, "multi_wish");
        this.items = items;
    }

    @Override
    public void grant(ServerPlayer player) {
        for (Tuple<String, Integer> itemInfo : items) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemInfo.getA()));
            if (item != null) {
                player.getInventory().add(new ItemStack(item, itemInfo.getB()));
            } else {
                LogUtil.warn(Env.COMMON, "Item with id " + itemInfo.getA() + " not found.");
            }
        }
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, MultiItemWish.class);
    }
}
