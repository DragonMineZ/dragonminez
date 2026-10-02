package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
		giveReward(player, 1.0);
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
		if (item == null) return;
		int scaledCount = scaledCount(rewardMultiplier);
		if (scaledCount <= 0) return;
		ItemStack stack = new ItemStack(item, scaledCount);
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
	}

	public int scaledCount(double rewardMultiplier) {
		if (count <= 0) return 0;
		return Math.max(1, (int) Math.round(count * rewardMultiplier));
	}

	@Override
	public Component getDescription() {
		return describe(count);
	}

	@Override
	public Component getDescription(double rewardMultiplier) {
		return describe(scaledCount(rewardMultiplier));
	}

	private Component describe(int shownCount) {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.item",
				shownCount,
				itemName()
		);
	}

	public MutableComponent itemName() {
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		if (id == null) return Component.literal(itemId);
		Item item = ForgeRegistries.ITEMS.getValue(id);
		if (item == null || item == Items.AIR) return Component.translatable("item." + id.toLanguageKey());
		return new ItemStack(item).getHoverName().copy();
	}
}
