package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import com.dragonminez.common.wish.Wish;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReCustomizeWish extends Wish {

	private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

	public ReCustomizeWish(String name, String description) {
		super(name, description, "recustomize");
	}

	@Override
	public void grant(ServerPlayer player) {
		PENDING.add(player.getUUID());
		NetworkHandler.sendToPlayer(new OpenRecustomizeS2C(), player);
	}

	public static boolean consume(ServerPlayer player) {
		return PENDING.remove(player.getUUID());
	}

}
