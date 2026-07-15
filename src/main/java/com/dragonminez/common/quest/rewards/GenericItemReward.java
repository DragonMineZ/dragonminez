package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
@Setter
public class GenericItemReward extends QuestReward {
	protected GenericItemDTO itemReward;

	public GenericItemReward(GenericItemDTO itemReward) {
		super(RewardType.GENERIC_ITEM);
		this.itemReward = itemReward;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		giveReward(player, 1.0);
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemReward.getItemId()));
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
		if (itemReward.getCount() <= 0) return 0;
		return Math.max(1, (int) Math.round(itemReward.getCount() * rewardMultiplier));
	}

	@Override
	public Component getDescription() {
		return describe(itemReward.getCount());
	}

	@Override
	public Component getDescription(double rewardMultiplier) {
		return describe(scaledCount(rewardMultiplier));
	}

	private Component describe(int shownCount) {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.item",
				shownCount,
				Component.translatable(
						"item." + ResourceLocation.parse(itemReward.getItemId()).toLanguageKey()
				)
		);
	}
}
