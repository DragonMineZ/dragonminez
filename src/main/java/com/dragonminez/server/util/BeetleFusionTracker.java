package com.dragonminez.server.util;

import com.dragonminez.common.init.item.consumables.MediBugBeetleItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BeetleFusionTracker {
	private static final double PAIR_RANGE = 20.0D;
	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	private record Pending(int pairId, long eatenAt, long expiresAt) {
	}

	private BeetleFusionTracker() {
	}

	public static void onBeetleEaten(ServerPlayer player, int pairId) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		if (!canFuse(data)) {
			player.displayClientMessage(Component.translatable("message.dragonminez.fusion.beetle.unavailable"), true);
			return;
		}
		long now = player.getServer().getTickCount();
		PENDING.put(player.getUUID(), new Pending(pairId, now, now + MediBugBeetleItem.PAIRED_EAT_LOCK_TICKS));
		if (!tryPair(player, data)) player.displayClientMessage(Component.translatable("message.dragonminez.fusion.beetle.waiting"), true);
	}

	public static void tick(ServerPlayer player, StatsData data) {
		Pending pending = PENDING.get(player.getUUID());
		if (pending == null) return;
		if (player.getServer().getTickCount() > pending.expiresAt()) {
			PENDING.remove(player.getUUID());
			if (canFuse(data)) player.displayClientMessage(Component.translatable("message.dragonminez.fusion.beetle.expired"), true);
			return;
		}
		if (!canFuse(data)) {
			PENDING.remove(player.getUUID());
			return;
		}
		tryPair(player, data);
	}

	public static void clear(UUID playerId) {
		PENDING.remove(playerId);
	}

	private static boolean tryPair(ServerPlayer player, StatsData data) {
		Pending mine = PENDING.get(player.getUUID());
		if (mine == null) return false;
		long now = player.getServer().getTickCount();

		for (ServerPlayer other : player.serverLevel().getEntitiesOfClass(ServerPlayer.class,
				player.getBoundingBox().inflate(PAIR_RANGE), p -> p != player)) {
			Pending theirs = PENDING.get(other.getUUID());
			if (theirs == null || theirs.pairId() != mine.pairId() || now > theirs.expiresAt()) continue;
			if (player.distanceTo(other) > PAIR_RANGE) continue;
			StatsData otherData = StatsProvider.get(StatsCapability.INSTANCE, other).orElse(null);
			if (otherData == null || !canFuse(otherData)) continue;

			PENDING.remove(player.getUUID());
			PENDING.remove(other.getUUID());
			boolean playerLeads = mine.eatenAt() <= theirs.eatenAt();
			if (playerLeads) FusionLogic.startPotaraPose(player, data, other, otherData, true);
			else FusionLogic.startPotaraPose(other, otherData, player, data, true);
			return true;
		}
		return false;
	}

	private static boolean canFuse(StatsData data) {
		return !data.getStatus().isFused()
				&& data.getStatus().getFusionPartnerUUID() == null
				&& data.getStatus().getPotaraPoseTimer() == 0
				&& data.getStatus().isHasCreatedCharacter();
	}
}
