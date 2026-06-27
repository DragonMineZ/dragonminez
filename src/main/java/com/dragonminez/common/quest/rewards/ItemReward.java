package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
public class ItemReward extends QuestReward {
	private final String itemId;
	private final int count;

	public ItemReward(ItemStack itemStack) {
		super(RewardType.ITEM);
		this.itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
		this.count = itemStack.getCount();
	}

	@Override
	public void giveReward(ServerPlayer player) {
		Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
		player.addItem(new ItemStack(item, count));
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.item",
				count,
				Component.translatable(
						"item." + ResourceLocation.parse(itemId).toLanguageKey()
				)
		);
	}
}
