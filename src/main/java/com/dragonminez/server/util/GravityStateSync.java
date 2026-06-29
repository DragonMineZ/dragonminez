package com.dragonminez.server.util;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.GravityZoneSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GravityStateSync {

	private record Snapshot(float machineGravity, float environmentalGravity, float netGravity,
							float statMult, float tpGravityMult, int idealWeight, int totalWeight,
							float loadRatio, float weightTpMult, int zone) {}

	private static final Map<UUID, Snapshot> LAST = new HashMap<>();

	private GravityStateSync() {}

	public static void sync(ServerPlayer player) {
		Snapshot now = build(player);
		if (changed(LAST.get(player.getUUID()), now)) {
			LAST.put(player.getUUID(), now);
			NetworkHandler.sendToPlayer(new GravityZoneSyncS2C(
					now.machineGravity, now.environmentalGravity, now.netGravity, now.statMult,
					now.tpGravityMult, now.idealWeight, now.totalWeight, now.loadRatio,
					now.weightTpMult, now.zone), player);
		}
	}

	public static void clear(UUID playerId) {
		LAST.remove(playerId);
	}

	private static Snapshot build(ServerPlayer player) {
		double tpGravityMult = StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(StatsData::getTpGravityMultiplier).orElse(1.0);
		return new Snapshot(
				(float) GravityLogic.getMachineGravity(player),
				(float) GravityLogic.getGravityMultiplier(player),
				(float) GravityLogic.getPenalizationGravity(player),
				(float) (1.0 - GravityLogic.getStatReduction(player)),
				(float) tpGravityMult,
				GravityLogic.getIdealWeight(player),
				GravityLogic.getTotalWeight(player),
				(float) GravityLogic.getLoadRatio(player),
				(float) GravityLogic.getWeightTpMultiplier(player),
				GravityLogic.getTrainingZone(player));
	}

	private static boolean changed(Snapshot a, Snapshot b) {
		if (a == null) return true;
		return Math.abs(a.machineGravity - b.machineGravity) >= 0.5f
				|| Math.abs(a.environmentalGravity - b.environmentalGravity) >= 0.1f
				|| Math.abs(a.netGravity - b.netGravity) >= 0.1f
				|| Math.abs(a.statMult - b.statMult) >= 0.01f
				|| Math.abs(a.tpGravityMult - b.tpGravityMult) >= 0.01f
				|| Math.abs(a.weightTpMult - b.weightTpMult) >= 0.01f
				|| Math.abs(a.loadRatio - b.loadRatio) >= 0.02f
				|| a.idealWeight != b.idealWeight
				|| a.totalWeight != b.totalWeight
				|| a.zone != b.zone;
	}
}
