package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemListWish extends Wish {
	private final List<? extends GenericItemDTO> items;

	public ItemListWish(String name, String description, List<GenericItemDTO> items) {
		super(name, description, "item_list_wish");
		this.items = items;
	}

	public ItemListWish(String name, String description, RegistryObject<? extends Item> item) {
		super(name, description, "item_list_wish");
		this.items = Collections.singletonList(new GenericItemDTO(item.getId(), 1));
	}

	public ItemListWish(String name, String description, RegistryObject<? extends Item> item, int count) {
		super(name, description, "item_list_wish");
		this.items = Collections.singletonList(new GenericItemDTO(item.getId(), count));
	}

	public ItemListWish(String name, String description, Map<ArmorItem.Type, RegistryObject<Item>> armorSet) {
		super(name, description, "item_list_wish");
		this.items = armorSet.keySet().stream().map(key -> new GenericItemDTO(armorSet.get(key).getId(), 1)).collect(Collectors.toList());
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
