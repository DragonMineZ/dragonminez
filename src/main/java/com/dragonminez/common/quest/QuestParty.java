package com.dragonminez.common.quest;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class QuestParty {
	public static final double SHARE_RANGE = 128.0;
	private static final double SHARE_RANGE_SQR = SHARE_RANGE * SHARE_RANGE;

	private QuestParty() {
	}

	public static boolean inShareRange(ServerPlayer a, ServerPlayer b) {
		if (a == null || b == null) return false;
		if (a == b) return true;
		return a.level() == b.level() && a.distanceToSqr(b) <= SHARE_RANGE_SQR;
	}

	public static PlayerQuestData questData(ServerPlayer player) {
		if (player == null || StorageManager.isLoadPending(player)) return null;
		return StatsProvider.get(StatsCapability.INSTANCE, player).resolve()
				.map(StatsData::getPlayerQuestData).orElse(null);
	}

	public static boolean isActive(ServerPlayer player, String questKey) {
		PlayerQuestData pqd = questData(player);
		return pqd != null && pqd.isQuestAccepted(questKey);
	}

	public static List<ServerPlayer> nearbyMembers(ServerPlayer actor) {
		List<ServerPlayer> nearby = new ArrayList<>();
		for (ServerPlayer member : PartyManager.getAllPartyMembers(actor)) {
			if (member == actor || inShareRange(actor, member)) nearby.add(member);
		}
		if (!nearby.contains(actor)) nearby.add(0, actor);
		return nearby;
	}

	public static List<ServerPlayer> participants(ServerPlayer actor, String questKey) {
		List<ServerPlayer> participants = new ArrayList<>();
		for (ServerPlayer member : nearbyMembers(actor)) {
			if (isActive(member, questKey)) participants.add(member);
		}
		return participants;
	}

	public static List<ServerPlayer> allParticipants(ServerPlayer player, String questKey) {
		List<ServerPlayer> participants = new ArrayList<>();
		for (ServerPlayer member : PartyManager.getAllPartyMembers(player)) {
			if (isActive(member, questKey)) participants.add(member);
		}
		if (!participants.contains(player) && isActive(player, questKey)) participants.add(0, player);
		return participants;
	}

	public static int countItem(ServerPlayer player, Item item) {
		if (player == null || item == null) return 0;
		Container inventory = player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isDeliverable(stack, item)) count += stack.getCount();
		}
		return count;
	}

	public static int countItemById(ServerPlayer player, String itemId) {
		if (player == null || itemId == null) return 0;
		ResourceLocation id = ResourceLocation.tryParse(itemId);
		if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return 0;
		Item item = ForgeRegistries.ITEMS.getValue(id);
		Container inventory = player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) count += stack.getCount();
		}
		return count;
	}

	public static int consumeItem(ServerPlayer player, Item item, int amount) {
		if (player == null || item == null || amount <= 0) return 0;
		Container inventory = player.getInventory();
		int remaining = amount;
		for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!isDeliverable(stack, item)) continue;
			int take = Math.min(stack.getCount(), remaining);
			stack.shrink(take);
			remaining -= take;
		}
		if (remaining != amount) inventory.setChanged();
		return amount - remaining;
	}

	private static boolean isDeliverable(ItemStack stack, Item item) {
		return !stack.isEmpty() && stack.is(item) && !stack.isEnchanted() && !stack.hasCustomHoverName();
	}
}
