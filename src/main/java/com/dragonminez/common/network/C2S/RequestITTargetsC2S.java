package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.ITTargetEntry;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenITMenuS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.MasterLocation;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestITTargetsC2S {
	private static final int MENU_SKILL_LEVEL = 5;
	private static final int CROSS_DIMENSION_SKILL_LEVEL = 10;
	private static final double BP_LOWER_RATIO = 0.90;
	private static final double BP_UPPER_RATIO = 1.25;

	public RequestITTargetsC2S() {}

	public RequestITTargetsC2S(FriendlyByteBuf ignored) {}

	public void encode(FriendlyByteBuf ignored) {}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int skillLevel = data.getSkills().getSkillLevel("instant_transmission");
				if (skillLevel < MENU_SKILL_LEVEL) return;

				String currentDim = player.level().dimension().location().toString();
				List<ITTargetEntry> entries = new ArrayList<>();
				Set<UUID> listedPlayers = new HashSet<>();

				for (MasterLocation master : data.getCharacter().getInteractedMasters().values()) {
					boolean reachable = skillLevel >= CROSS_DIMENSION_SKILL_LEVEL || master.getDimension().equals(currentDim);
					entries.add(new ITTargetEntry(ITTargetEntry.Type.MASTER, master.getMasterId(), master.getDisplayName(), master.getDimension(), reachable));
				}

				for (ServerPlayer member : PartyManager.getAllPartyMembers(player)) {
					if (member.getUUID().equals(player.getUUID())) continue;
					StatsData memberData = StatsProvider.get(StatsCapability.INSTANCE, member).orElse(null);
					if (memberData == null || !memberData.getStatus().isHasCreatedCharacter()) continue;
					if (TransformationsHelper.isInstantTransmissionBlocked(memberData)) continue;

					String memberDim = member.level().dimension().location().toString();
					boolean reachable = skillLevel >= CROSS_DIMENSION_SKILL_LEVEL || memberDim.equals(currentDim);
					entries.add(new ITTargetEntry(ITTargetEntry.Type.PARTY, member.getUUID().toString(), member.getGameProfile().getName(), memberDim, reachable));
					listedPlayers.add(member.getUUID());
				}

				long selfBp = data.getBattlePowerExact();
				int rangePerLevel = ConfigManager.getServerConfig().getGameplay().getInstantTransmissionPlayerRangePerLevel();
				double maxRange = (double) rangePerLevel * skillLevel;

				for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
					if (other.getUUID().equals(player.getUUID()) || listedPlayers.contains(other.getUUID())) continue;
					StatsData otherData = StatsProvider.get(StatsCapability.INSTANCE, other).orElse(null);
					if (otherData == null || !otherData.getStatus().isHasCreatedCharacter()) continue;
					if (!isValidExternalTarget(player, data, selfBp, other, otherData, maxRange)) continue;

					entries.add(new ITTargetEntry(ITTargetEntry.Type.EXTERNAL, other.getUUID().toString(),
							other.getGameProfile().getName(), currentDim, true));
				}

				NetworkHandler.sendToPlayer(new OpenITMenuS2C(entries), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	public static boolean isValidExternalTarget(ServerPlayer self, StatsData selfData, long selfBp, ServerPlayer target, StatsData targetData, double maxRange) {
		if (!target.level().dimension().equals(self.level().dimension())) return false;
		if (self.position().distanceTo(target.position()) > maxRange) return false;
		if (target.isCreative() || target.isSpectator()) return false;
		if (TransformationsHelper.isInstantTransmissionBlocked(targetData)) return false;
		if (targetData.getStatus().isFused() && !targetData.getStatus().isFusionLeader()) return false;

		long targetBp = targetData.getBattlePowerExact();
		return targetBp >= (long) (selfBp * BP_LOWER_RATIO) && targetBp <= (long) (selfBp * BP_UPPER_RATIO);
	}
}
