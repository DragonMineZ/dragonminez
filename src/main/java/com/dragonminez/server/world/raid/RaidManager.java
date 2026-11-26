package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.data.PartySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class RaidManager {

	private RaidManager() {}
	public static Raid startRaid(ServerLevel level, BlockPos center, ServerPlayer initiator, String typeId) {
		RaidType type = RaidTypes.getOrDefault(typeId);
		if (type == null) {
			LogUtil.warn(Env.SERVER, "Tried to start unknown raid type '{}'", typeId);
			return null;
		}

		RaidSavedData data = RaidSavedData.get(level.getServer());

		// Don't stack raids on top of one another.
		if (data.findNearbyRaid(level, center, type.getActivationRadius()) != null) {
			return null;
		}

		Set<UUID> participants = gatherParticipants(level.getServer(), initiator);
		Raid raid = new Raid(UUID.randomUUID(), type.getId(), level.dimension(), center, participants,
				resolveDifficulty(initiator));
		data.addRaid(raid);

		LogUtil.info(Env.SERVER, "Started raid '{}' at {} with {} participant(s)",
				type.getId(), center, participants.size());
		return raid;
	}
	private static Difficulty resolveDifficulty(ServerPlayer initiator) {
		return StatsProvider.get(StatsCapability.INSTANCE, initiator)
				.map(data -> data.getPlayerQuestData().getDifficulty())
				.orElse(Difficulty.NORMAL);
	}

	private static Set<UUID> gatherParticipants(MinecraftServer server, ServerPlayer initiator) {
		Set<UUID> participants = new LinkedHashSet<>();
		participants.add(initiator.getUUID());

		PartySavedData parties = PartySavedData.get(server);
		PartySavedData.PartyInstance party = parties.getPartyOf(initiator.getUUID());
		if (party != null) {
			participants.addAll(party.getMembers());
		}
		return participants;
	}

	public static void tick(ServerLevel level) {
		RaidSavedData.get(level.getServer()).tick(level);
	}

	public static boolean cancelNearbyRaid(ServerLevel level, BlockPos pos, double range) {
		Raid raid = RaidSavedData.get(level.getServer()).findNearbyRaid(level, pos, range);
		if (raid == null) return false;
		raid.cancel(level);
		return true;
	}

	public static Raid findNearbyRaid(ServerLevel level, BlockPos pos, double range) {
		return RaidSavedData.get(level.getServer()).findNearbyRaid(level, pos, range);
	}
}
