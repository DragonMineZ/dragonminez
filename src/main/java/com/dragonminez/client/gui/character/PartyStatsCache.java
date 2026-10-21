package com.dragonminez.client.gui.character;

import com.dragonminez.common.network.PartyPackets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class PartyStatsCache {

	private static final Map<UUID, PartyPackets.MemberStats> MEMBERS = new LinkedHashMap<>();

	private PartyStatsCache() {}

	public static void accept(List<PartyPackets.MemberStats> members) {
		MEMBERS.clear();
		for (PartyPackets.MemberStats member : members) MEMBERS.put(member.id(), member);
	}

	public static void clear() {
		MEMBERS.clear();
	}

	public static PartyPackets.MemberStats get(UUID id) {
		return MEMBERS.get(id);
	}

	public static List<PartyPackets.MemberStats> all() {
		return new ArrayList<>(MEMBERS.values());
	}
}
