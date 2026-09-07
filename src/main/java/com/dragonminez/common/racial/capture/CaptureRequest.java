package com.dragonminez.common.racial.capture;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RacialRequestS2C;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CaptureRequest {

	private CaptureRequest() {
	}

	public static void send(ServerPlayer requester, ServerPlayer target, int timeoutSeconds) {
		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
			long expiresAt = System.currentTimeMillis() + (long) timeoutSeconds * 1000L;
			targetData.getRacialData().setPendingCaptureRequest(
					new PendingCapture(requester.getUUID(), requester.getGameProfile().getName(), expiresAt));
		});

		NetworkHandler.sendToPlayer(new RacialRequestS2C(requester.getGameProfile().getName(), timeoutSeconds), target);
		requester.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.request_sent",
				target.getGameProfile().getName()), true);
	}

	public static void reply(ServerPlayer target, boolean accepted) {
		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
			PendingCapture pending = targetData.getRacialData().getPendingCaptureRequest();
			targetData.getRacialData().setPendingCaptureRequest(null);
			if (pending == null) return;

			if (pending.isExpired()) {
				target.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.request_expired"), true);
				return;
			}

			ServerPlayer requester = target.getServer().getPlayerList().getPlayer(pending.requesterId());
			if (requester == null) return;

			if (!accepted) {
				requester.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.request_rejected",
						target.getGameProfile().getName()), true);
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, requester).ifPresent(requesterData ->
					NamekAssimilation.applyAssimilation(requester, requesterData, target));
		});
	}
}
