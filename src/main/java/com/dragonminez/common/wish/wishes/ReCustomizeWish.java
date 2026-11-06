package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import com.dragonminez.common.wish.Wish;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReCustomizeWish extends Wish {

	private static final int PENDING_EXPIRY_TICKS = 15 * 60 * 20;
	private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();

	public ReCustomizeWish(String name, String description) {
		super(name, description, "recustomize");
	}

	@Override
	public void grant(ServerPlayer player) {
		PENDING.put(player.getUUID(), player.server.getTickCount());
		NetworkHandler.sendToPlayer(new OpenRecustomizeS2C(), player);
	}

	public static boolean isPending(ServerPlayer player) {
		Integer grantedAt = PENDING.get(player.getUUID());
		return grantedAt != null && player.server.getTickCount() - grantedAt <= PENDING_EXPIRY_TICKS;
	}

	public static boolean consume(ServerPlayer player) {
		Integer grantedAt = PENDING.remove(player.getUUID());
		return grantedAt != null && player.server.getTickCount() - grantedAt <= PENDING_EXPIRY_TICKS;
	}

	public static void clear(UUID uuid) {
		PENDING.remove(uuid);
	}

}
