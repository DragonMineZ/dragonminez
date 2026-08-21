package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemListWish extends Wish {
	private final List<? extends GenericItemDTO> items;

	public ItemListWish(String name, String description, List<GenericItemDTO> items) {
		super(name, description, "item_list_wish");
		this.items = items;
	}

	@Override
	public void grant(ServerPlayer player) {
		for (var itemInfo : items) {
			giveOrDrop(player, itemInfo.getItemStack());
		}
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
	}

	@Override
	public String toJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this, ItemListWish.class);
	}
}
