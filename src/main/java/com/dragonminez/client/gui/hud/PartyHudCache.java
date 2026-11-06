package com.dragonminez.client.gui.hud;

import com.dragonminez.common.network.PartyPackets;

import java.util.List;

public final class PartyHudCache {
	private static final long STALE_MILLIS = 2500L;

	private static volatile List<PartyPackets.HudMember> members = List.of();
	private static volatile long receivedAt;

	private PartyHudCache() {}

	public static void accept(List<PartyPackets.HudMember> incoming) {
		members = incoming == null ? List.of() : List.copyOf(incoming);
		receivedAt = System.currentTimeMillis();
	}

	public static List<PartyPackets.HudMember> current() {
		if (System.currentTimeMillis() - receivedAt > STALE_MILLIS) return List.of();
		return members;
	}

	public static void clear() {
		members = List.of();
		receivedAt = 0L;
	}
}
