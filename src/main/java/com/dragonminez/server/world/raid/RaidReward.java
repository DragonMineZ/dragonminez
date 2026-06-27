package com.dragonminez.server.world.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RaidReward {

	private final List<Supplier<ItemStack>> itemRewards;

	private RaidReward(List<Supplier<ItemStack>> itemRewards) {
		this.itemRewards = itemRewards;
	}

	/** Grants this reward to every participant that completed the raid. */
	public void grant(ServerLevel level, List<ServerPlayer> participants, BlockPos center) {
		for (ServerPlayer player : participants) {
			for (Supplier<ItemStack> supplier : itemRewards) {
				ItemStack stack = supplier.get();
				if (stack.isEmpty()) continue;
				if (!player.getInventory().add(stack)) {
					player.drop(stack, false);
				}
			}
		}
	}

	public boolean isEmpty() {
		return itemRewards.isEmpty();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Supplier<ItemStack>> itemRewards = new ArrayList<>();

		public Builder item(Supplier<? extends Item> item, int count) {
			this.itemRewards.add(() -> new ItemStack(item.get(), count));
			return this;
		}

		public RaidReward build() {
			return new RaidReward(List.copyOf(itemRewards));
		}
	}
}
