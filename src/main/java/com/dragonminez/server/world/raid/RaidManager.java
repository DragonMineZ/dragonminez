package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
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

	/**
	 * Starts a raid centred on {@code center}, with {@code initiator} and their party (if any) as
	 * participants.
	 *
	 * <p><b>Structure integration:</b> this is the hook to call once structure detection exists — when a
	 * player enters a raid-enabled structure, call this with the structure's centre. For now it is driven
	 * by the {@code /dmzraid} debug command.</p>
	 *
	 * @return the started raid, or {@code null} if one could not be started (unknown type, or a raid is
	 * already active nearby).
	 */
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
		Raid raid = new Raid(UUID.randomUUID(), type.getId(), level.dimension(), center, participants);
		data.addRaid(raid);

		LogUtil.info(Env.SERVER, "Started raid '{}' at {} with {} participant(s)",
				type.getId(), center, participants.size());
		return raid;
	}

	/** Collects the initiator plus their party members (online or not) as participants. */
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

	/** Ticks all raids belonging to the level. Invoked once per server level tick. */
	public static void tick(ServerLevel level) {
		RaidSavedData.get(level.getServer()).tick(level);
	}

	/** Cancels the raid nearest to {@code pos} within {@code range}. Returns {@code true} if one was cancelled. */
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
